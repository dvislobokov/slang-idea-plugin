package org.slang.idea.toolchain

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/**
 * Toolchain paths set by the user (Settings | Languages & Frameworks | Slang).
 * Empty means "detect" (see [SlangToolchain]).
 */
@Service(Service.Level.APP)
@State(name = "SlangToolchainSettings", storages = [Storage("slang.xml")])
class SlangToolchainSettings : PersistentStateComponent<SlangToolchainSettings.State> {
    class State {
        /** Path of the `slang` executable. */
        var slangPath: String = ""

        /** Path of the `slang-lsp` executable. */
        var lspPath: String = ""

        /** SLANG_HOME (its `std/` is the standard library when `slang env SLANG_STD` is unavailable). */
        var slangHome: String = ""
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        @JvmStatic
        fun getInstance(): SlangToolchainSettings = service()
    }
}
