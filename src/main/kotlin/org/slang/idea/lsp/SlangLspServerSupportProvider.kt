package org.slang.idea.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspFormattingSupport
import org.slang.idea.lang.SlangFileType
import org.slang.idea.toolchain.SlangToolchain
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Hosting of slang-lsp through the platform LSP API (com.intellij.platform.lsp),
 * the way the Go plugin hosts gopls. Loaded only in IDEs with
 * com.intellij.modules.ultimate (META-INF/slang-lsp.xml).
 *
 * TODO(verify): every LSP API member used here against
 * https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html
 * for 2024.2 (242); the module is closed source, so these signatures were
 * written from the documentation, not compiled here.
 */

/** Starts one slang-lsp per project when a `.sl` file is opened. */
class SlangLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (!isSlangFile(file)) return
        if (SlangToolchain.lspCommandLine() == null) {
            notifyMissingToolchain(project)
            return
        }
        serverStarter.ensureServerStarted(SlangLspServerDescriptor(project))
    }

    private fun notifyMissingToolchain(project: Project) {
        if (!warned.compareAndSet(false, true)) return
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Slang")
            .createNotification(
                "Slang language server not found",
                "Install the toolchain (scripts/install.sh from the slang repository) or set the paths in " +
                    "Settings | Languages &amp; Frameworks | Slang. Highlighting and structure work without it.",
                NotificationType.WARNING,
            )
            .notify(project)
    }

    private companion object {
        val warned = AtomicBoolean(false)
    }
}

internal fun isSlangFile(file: VirtualFile): Boolean = file.fileType == SlangFileType

/**
 * slang-lsp over stdio. Its capabilities (lsp/src/lib.rs `capabilities()`):
 * hover, completion (`.`), signature help, inlay hints, code actions,
 * formatting (document and range), workspace/document symbols, document
 * highlight, definition, references, rename with prepare, folding ranges,
 * semantic tokens. The platform enables the features it supports by default;
 * the overrides below only state the choices.
 */
class SlangLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Slang") {
    override fun isSupportedFile(file: VirtualFile): Boolean = isSlangFile(file)

    override fun createCommandLine(): GeneralCommandLine =
        (SlangToolchain.lspCommandLine() ?: error("slang-lsp not found"))
            .withWorkDirectory(project.basePath)

    /** `slang` is the language id slang-lsp expects (editors/vscode uses it too). */
    override fun getLanguageId(file: VirtualFile): String = "slang"

    // TODO(verify): in 2024.2 these are `open val`s of LspServerDescriptor with
    // these types; 2025.1 moved them to `lspCustomization`. Defaults are all on.
    override val lspGoToDefinitionSupport: Boolean = true

    override val lspHoverSupport: Boolean = true

    /** `slang fmt` through textDocument/formatting is the only Slang formatter. */
    override val lspFormattingSupport: LspFormattingSupport = object : LspFormattingSupport() {
        override fun shouldFormatThisFileExclusivelyByServer(
            file: VirtualFile,
            ideCanFormatThisFileItself: Boolean,
            serverExplicitlyWantsToFormatThisFile: Boolean,
        ): Boolean = isSlangFile(file)
    }
}
