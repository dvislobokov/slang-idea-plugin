package org.zell.idea.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.google.gson.JsonObject
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCommandsSupport
import com.intellij.platform.lsp.api.customization.LspFormattingSupport
import org.eclipse.lsp4j.Command
import org.zell.idea.lang.ZellFileType
import org.zell.idea.toolchain.ZellToolchain
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Hosting of zell-lsp through the platform LSP API (com.intellij.platform.lsp),
 * the way the Go plugin hosts gopls. Loaded only in IDEs with
 * com.intellij.modules.ultimate (META-INF/zell-lsp.xml).
 *
 * TODO(verify): every LSP API member used here against
 * https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html
 * for 2024.2 (242); the module is closed source, so these signatures were
 * written from the documentation, not compiled here.
 */

/** Starts one zell-lsp per project when a `.zl` file is opened. */
class ZellLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (!isZellFile(file)) return
        if (ZellToolchain.lspCommandLine() == null) {
            notifyMissingToolchain(project)
            return
        }
        serverStarter.ensureServerStarted(ZellLspServerDescriptor(project))
    }

    private fun notifyMissingToolchain(project: Project) {
        if (!warned.compareAndSet(false, true)) return
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Zell")
            .createNotification(
                "Zell language server not found",
                "Install the toolchain (scripts/install.sh from the zell repository) or set the paths in " +
                    "Settings | Languages &amp; Frameworks | Zell. Highlighting and structure work without it.",
                NotificationType.WARNING,
            )
            .notify(project)
    }

    private companion object {
        val warned = AtomicBoolean(false)
    }
}

internal fun isZellFile(file: VirtualFile): Boolean = file.fileType == ZellFileType

/**
 * zell-lsp over stdio. Its capabilities (lsp/src/lib.rs `capabilities()`):
 * hover, completion (`.`), signature help, inlay hints, code actions,
 * formatting (document and range), workspace/document symbols, document
 * highlight, definition, references, rename with prepare, folding ranges,
 * semantic tokens, code lens (commands in [ZellLspCommands]). The platform enables the features it supports by default;
 * the overrides below only state the choices.
 */
class ZellLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Zell") {
    override fun isSupportedFile(file: VirtualFile): Boolean = isZellFile(file)

    override fun createCommandLine(): GeneralCommandLine =
        (ZellToolchain.lspCommandLine() ?: error("zell-lsp not found"))
            .withWorkDirectory(project.basePath)

    /** `zell` is the language id zell-lsp expects (editors/vscode uses it too). TODO(verify): overridable in 242. */
    override fun getLanguageId(file: VirtualFile): String = "zell"

    /**
     * `initializationOptions` (upstream docs/TOOLING.md, lsp/src/lib.rs `diag_mode`):
     * push diagnostics, as recommended for IntelliJ (the platform client is
     * push-based; `auto` would pick pull if the client announced
     * `textDocument.diagnostic`, and the server never sends both), and the
     * inlay hint kinds editors/vscode sends as `zell.inlayHints.*`.
     * TODO(verify): `createInitializationOptions(): Any?` in 242 (serialized by lsp4j's Gson).
     */
    override fun createInitializationOptions(): Any = JsonObject().apply {
        add("diagnostics", JsonObject().apply { addProperty("mode", "push") })
        add("inlayHints", JsonObject().apply {
            add("types", JsonObject().apply { addProperty("enabled", true) })
            add("parameterNames", JsonObject().apply { addProperty("enabled", true) })
        })
    }

    /**
     * Client-side commands of zell-lsp's code lenses (`zell.runTest`,
     * `zell.debugTest`, `zell.run`, `zell.showReferences`); see [ZellLspCommands].
     * TODO(verify): `lspCommandsSupport: LspCommandsSupport?` with
     * `executeCommand(server, contextFile, command)` in 242, and from which
     * build the platform renders LSP code lenses at all (newer than 242 is
     * possible; until then these commands are simply never invoked).
     */
    override val lspCommandsSupport: LspCommandsSupport = object : LspCommandsSupport() {
        override fun executeCommand(server: LspServer, contextFile: VirtualFile, command: Command) {
            if (!ZellLspCommands.execute(project, command.command, command.arguments.orEmpty())) {
                super.executeCommand(server, contextFile, command)
            }
        }
    }

    // TODO(verify): in 2024.2 these are `open val`s of LspServerDescriptor with
    // these types; 2025.1 moved them to `lspCustomization`. Defaults are all on.
    override val lspGoToDefinitionSupport: Boolean = true

    override val lspHoverSupport: Boolean = true

    /** `zell fmt` through textDocument/formatting is the only Zell formatter. */
    override val lspFormattingSupport: LspFormattingSupport = object : LspFormattingSupport() {
        override fun shouldFormatThisFileExclusivelyByServer(
            file: VirtualFile,
            ideCanFormatThisFileItself: Boolean,
            serverExplicitlyWantsToFormatThisFile: Boolean,
        ): Boolean = isZellFile(file)
    }
}
