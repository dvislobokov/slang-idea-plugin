package org.slang.idea.toolchain

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
 * Locates the Slang toolchain: explicit paths from [SlangToolchainSettings],
 * then PATH, then `$SLANG_HOME/toolchain/current/bin` and
 * `~/.slang/toolchain/current/bin` (the layout of scripts/install.sh).
 */
object SlangToolchain {
    private val LOG = logger<SlangToolchain>()

    /** Published on the application bus after the toolchain settings change. */
    @JvmField
    val TOPIC: Topic<Listener> = Topic.create("Slang toolchain changed", Listener::class.java)

    fun interface Listener {
        fun toolchainChanged()
    }

    @Volatile
    private var cachedStd: Path? = null

    @Volatile
    private var stdResolved = false

    private val settings: SlangToolchainSettings.State
        get() = SlangToolchainSettings.getInstance().state

    /** The `slang` compiler, or null if it cannot be found. */
    fun slangExecutable(): Path? = locate(settings.slangPath, "slang")

    /** The `slang-lsp` language server, or null if it cannot be found. */
    fun lspExecutable(): Path? =
        locate(settings.lspPath, "slang-lsp")
            // `slang lsp` execs the sibling slang-lsp, so look next to slang too.
            ?: slangExecutable()?.resolveSibling(exe("slang-lsp"))?.takeIf(Files::isExecutable)

    /**
     * The command that starts the language server on stdio: `slang-lsp`, or
     * `slang lsp` (which execs a slang-lsp next to the compiler) when only the
     * compiler is known. Null when neither is installed.
     */
    fun lspCommandLine(): GeneralCommandLine? {
        lspExecutable()?.let { return GeneralCommandLine(it.toString()).withCharset(Charsets.UTF_8) }
        slangExecutable()?.let { return GeneralCommandLine(it.toString(), "lsp").withCharset(Charsets.UTF_8) }
        return null
    }

    /** SLANG_HOME: the setting, the environment variable, or `~/.slang` if it exists. */
    fun slangHome(): Path? {
        settings.slangHome.takeIf { it.isNotBlank() }?.let { return Paths.get(it) }
        System.getenv("SLANG_HOME")?.takeIf { it.isNotBlank() }?.let { return Paths.get(it) }
        return Paths.get(System.getProperty("user.home"), ".slang").takeIf(Files::isDirectory)
    }

    /**
     * The standard library sources: `slang env SLANG_STD` (authoritative, also
     * for source checkouts), else `SLANG_HOME/std`. The process runs at most
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
        val slang = slangExecutable() ?: return null
        return try {
            val out = ExecUtil.execAndGetOutput(GeneralCommandLine(slang.toString(), "env", "SLANG_STD"), 5_000)
            if (out.exitCode != 0 || out.isTimeout) return null
            // `slang env NAME` prints the bare value for a single name.
            out.stdout.trim().removeSurrounding("\"").takeIf { it.isNotEmpty() }
                ?.let { Paths.get(it) }?.takeIf(Files::isDirectory)
        } catch (e: Exception) {
            LOG.info("cannot run `$slang env SLANG_STD`", e)
            null
        }
    }

    private fun fallbackStd(): Path? {
        System.getenv("SLANG_STD")?.takeIf { it.isNotBlank() }?.let { Paths.get(it) }?.takeIf(Files::isDirectory)?.let { return it }
        return slangHome()?.resolve("std")?.takeIf(Files::isDirectory)
    }

    private fun locate(configured: String, name: String): Path? {
        if (configured.isNotBlank()) return Paths.get(configured).takeIf(Files::isExecutable)
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(name)?.toPath()?.let { return it }
        val homes = listOfNotNull(slangHome(), Paths.get(System.getProperty("user.home"), ".slang"))
        for (home in homes) {
            val candidate = home.resolve("toolchain").resolve("current").resolve("bin").resolve(exe(name))
            if (Files.isExecutable(candidate)) return candidate
        }
        return null
    }

    private fun exe(name: String): String = if (SystemInfo.isWindows) "$name.exe" else name
}
