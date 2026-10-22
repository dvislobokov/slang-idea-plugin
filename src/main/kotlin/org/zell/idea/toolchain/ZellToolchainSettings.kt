package org.zell.idea.toolchain

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/**
 * Toolchain paths set by the user (Settings | Languages & Frameworks | Zell).
 * Empty means "detect" (see [ZellToolchain]).
 */
@Service(Service.Level.APP)
@State(name = "ZellToolchainSettings", storages = [Storage("zell.xml")])
class ZellToolchainSettings : PersistentStateComponent<ZellToolchainSettings.State> {
    class State {
        /** Path of the `zell` executable. */
        var zellPath: String = ""

        /** Path of the `zell-lsp` executable. */
        var lspPath: String = ""

        /** ZELL_HOME (its `std/` is the standard library when `zell env ZELL_STD` is unavailable). */
        var zellHome: String = ""
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        @JvmStatic
        fun getInstance(): ZellToolchainSettings = service()
    }
}
