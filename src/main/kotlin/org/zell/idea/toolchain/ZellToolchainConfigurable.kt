package org.zell.idea.toolchain

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

/** Settings | Languages & Frameworks | Zell. */
class ZellToolchainConfigurable : BoundConfigurable("Zell") {
    private val state get() = ZellToolchainSettings.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        group("Toolchain") {
            row("zell:") {
                textFieldWithBrowseButton("Select the zell Executable", null, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
                    .bindText(state::zellPath)
                    .align(AlignX.FILL)
                    .comment("Empty: detect on PATH, then ~/.zell/toolchain/current/bin. Detected: ${ZellToolchain.zellExecutable() ?: "not found"}")
            }
            row("zell-lsp:") {
                textFieldWithBrowseButton("Select the zell-lsp Executable", null, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
                    .bindText(state::lspPath)
                    .align(AlignX.FILL)
                    .comment("Empty: detect on PATH or next to zell; without it the IDE runs `zell lsp`. Detected: ${ZellToolchain.lspExecutable() ?: "not found"}")
            }
            row("ZELL_HOME:") {
                textFieldWithBrowseButton("Select ZELL_HOME", null, FileChooserDescriptorFactory.createSingleFolderDescriptor())
                    .bindText(state::zellHome)
                    .align(AlignX.FILL)
                    .comment("The standard library comes from `zell env ZELL_STD`, else ZELL_HOME/std. Install: scripts/install.sh from the zell repository.")
            }
        }
    }

    override fun apply() {
        super.apply()
        ZellToolchain.toolchainChanged()
    }
}
