package org.slang.idea.toolchain

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

/** Settings | Languages & Frameworks | Slang. */
class SlangToolchainConfigurable : BoundConfigurable("Slang") {
    private val state get() = SlangToolchainSettings.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        group("Toolchain") {
            row("slang:") {
                textFieldWithBrowseButton("Select the slang Executable", null, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
                    .bindText(state::slangPath)
                    .align(AlignX.FILL)
                    .comment("Empty: detect on PATH, then ~/.slang/toolchain/current/bin. Detected: ${SlangToolchain.slangExecutable() ?: "not found"}")
            }
            row("slang-lsp:") {
                textFieldWithBrowseButton("Select the slang-lsp Executable", null, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
                    .bindText(state::lspPath)
                    .align(AlignX.FILL)
                    .comment("Empty: detect on PATH or next to slang; without it the IDE runs `slang lsp`. Detected: ${SlangToolchain.lspExecutable() ?: "not found"}")
            }
            row("SLANG_HOME:") {
                textFieldWithBrowseButton("Select SLANG_HOME", null, FileChooserDescriptorFactory.createSingleFolderDescriptor())
                    .bindText(state::slangHome)
                    .align(AlignX.FILL)
                    .comment("The standard library comes from `slang env SLANG_STD`, else SLANG_HOME/std. Install: scripts/install.sh from the slang repository.")
            }
        }
    }

    override fun apply() {
        super.apply()
        SlangToolchain.toolchainChanged()
    }
}
