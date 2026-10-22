package org.zell.idea.toolchain

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.messages.Topic
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Locates the Zell toolchain: explicit paths from [ZellToolchainSettings],
 * then PATH, then `$ZELL_HOME/toolchain/current/bin` and
 * `~/.zell/toolchain/current/bin` (the layout of scripts/install.sh).
 */
object ZellToolchain {
    private val LOG = logger<ZellToolchain>()

    /** Published on the application bus after the toolchain settings change. */
    @JvmField
    val TOPIC: Topic<Listener> = Topic.create("Zell toolchain changed", Listener::class.java)

    fun interface Listener {
        fun toolchainChanged()
    }

    @Volatile
    private var cachedStd: Path? = null

    @Volatile
    private var stdResolved = false

    private val settings: ZellToolchainSettings.State
        get() = ZellToolchainSettings.getInstance().state

    /** The `zell` compiler, or null if it cannot be found. */
    fun zellExecutable(): Path? = locate(settings.zellPath, "zell")

    /** The `zell-lsp` language server, or null if it cannot be found. */
    fun lspExecutable(): Path? =
        locate(settings.lspPath, "zell-lsp")
            // `zell lsp` execs the sibling zell-lsp, so look next to zell too.
            ?: zellExecutable()?.resolveSibling(exe("zell-lsp"))?.takeIf(Files::isExecutable)

    /**
     * The command that starts the language server on stdio: `zell-lsp`, or
     * `zell lsp` (which execs a zell-lsp next to the compiler) when only the
     * compiler is known. Null when neither is installed.
     */
    fun lspCommandLine(): GeneralCommandLine? {
        lspExecutable()?.let { return GeneralCommandLine(it.toString()).withCharset(Charsets.UTF_8) }
        zellExecutable()?.let { return GeneralCommandLine(it.toString(), "lsp").withCharset(Charsets.UTF_8) }
        return null
    }

    /** ZELL_HOME: the setting, the environment variable, or `~/.zell` if it exists. */
    fun zellHome(): Path? {
        settings.zellHome.takeIf { it.isNotBlank() }?.let { return Paths.get(it) }
        System.getenv("ZELL_HOME")?.takeIf { it.isNotBlank() }?.let { return Paths.get(it) }
        return Paths.get(System.getProperty("user.home"), ".zell").takeIf(Files::isDirectory)
    }

    /**
     * The standard library sources: `zell env ZELL_STD` (authoritative, also
     * for source checkouts), else `ZELL_HOME/std`. The process runs at most
     * once per toolchain change and never on the EDT.
     */
    fun stdRoot(): Path? {
        if (stdResolved) return cachedStd
        val app = ApplicationManager.getApplication()
        if (app != null && app.isDispatchThread) return fallbackStd()
        val std = stdFromCompiler() ?: fallbackStd()
        cachedStd = std
        stdResolved = true
        return std
    }

    /** Forgets cached lookups and notifies [TOPIC] subscribers (LSP restart, library roots). */
    fun toolchainChanged() {
        stdResolved = false
        cachedStd = null
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).toolchainChanged()
    }

    private fun stdFromCompiler(): Path? {
        val zell = zellExecutable() ?: return null
        return try {
            val out = ExecUtil.execAndGetOutput(GeneralCommandLine(zell.toString(), "env", "ZELL_STD"), 5_000)
            if (out.exitCode != 0 || out.isTimeout) return null
            // `zell env NAME` prints the bare value for a single name.
            out.stdout.trim().removeSurrounding("\"").takeIf { it.isNotEmpty() }
                ?.let { Paths.get(it) }?.takeIf(Files::isDirectory)
        } catch (e: Exception) {
            LOG.info("cannot run `$zell env ZELL_STD`", e)
            null
        }
    }

    private fun fallbackStd(): Path? {
        System.getenv("ZELL_STD")?.takeIf { it.isNotBlank() }?.let { Paths.get(it) }?.takeIf(Files::isDirectory)?.let { return it }
        return zellHome()?.resolve("std")?.takeIf(Files::isDirectory)
    }

    private fun locate(configured: String, name: String): Path? {
        if (configured.isNotBlank()) return Paths.get(configured).takeIf(Files::isExecutable)
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(name)?.toPath()?.let { return it }
        val homes = listOfNotNull(zellHome(), Paths.get(System.getProperty("user.home"), ".zell"))
        for (home in homes) {
            val candidate = home.resolve("toolchain").resolve("current").resolve("bin").resolve(exe(name))
            if (Files.isExecutable(candidate)) return candidate
        }
        return null
    }

    private fun exe(name: String): String = if (SystemInfo.isWindows) "$name.exe" else name
}
