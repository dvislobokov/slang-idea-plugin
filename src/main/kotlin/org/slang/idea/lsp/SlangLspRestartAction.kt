package org.slang.idea.lsp

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.intellij.platform.lsp.api.LspServerManager
import org.slang.idea.toolchain.SlangToolchain

/** Tools | Restart Slang Language Server. */
class SlangLspRestartAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        restart(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun restart(project: com.intellij.openapi.project.Project) {
            // TODO(verify): LspServerManager.stopAndRestartIfNeeded(Class) in 2024.2.
            LspServerManager.getInstance(project).stopAndRestartIfNeeded(SlangLspServerSupportProvider::class.java)
        }
    }

    /** Restarts the server in every open project when the toolchain settings change. */
    class ToolchainListener : SlangToolchain.Listener {
        override fun toolchainChanged() {
            for (project in ProjectManager.getInstance().openProjects) {
                if (!project.isDisposed) restart(project)
            }
        }
    }
}
