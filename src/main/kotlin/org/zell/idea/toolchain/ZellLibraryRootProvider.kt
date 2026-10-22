package org.zell.idea.toolchain

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.AdditionalLibraryRootsListener
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.zell.idea.lang.ZellIcons
import javax.swing.Icon

/**
 * Attaches the standard library sources ([ZellToolchain.stdRoot], i.e.
 * `zell env ZELL_STD`) as an external library, like GOROOT in the Go
 * plugin: PSI features (structure, Go to File/Symbol) see std; semantic
 * navigation into std comes from zell-lsp.
 */
class ZellLibraryRootProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> =
        listOfNotNull(stdRoot()?.let(::ZellStdLibrary))

    override fun getRootsToWatch(project: Project): Collection<VirtualFile> = listOfNotNull(stdRoot())

    private fun stdRoot(): VirtualFile? =
        ZellToolchain.stdRoot()?.let { LocalFileSystem.getInstance().findFileByNioFile(it) }

    /** Re-indexes std after the toolchain settings change. */
    class ToolchainListener : ZellToolchain.Listener {
        override fun toolchainChanged() {
            val app = ApplicationManager.getApplication()
            app.executeOnPooledThread {
                val newRoot = ZellToolchain.stdRoot()?.let { LocalFileSystem.getInstance().refreshAndFindFileByNioFile(it) }
                app.invokeLater {
                    WriteAction.run<RuntimeException> {
                        for (project in ProjectManager.getInstance().openProjects) {
                            if (project.isDisposed) continue
                            AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(
                                project, LIBRARY_NAME, emptyList(), listOfNotNull(newRoot), "Zell toolchain change",
                            )
                        }
                    }
                }
            }
        }
    }

    private class ZellStdLibrary(private val root: VirtualFile) : SyntheticLibrary(), ItemPresentation {
        override fun getSourceRoots(): Collection<VirtualFile> = listOf(root)

        override fun equals(other: Any?): Boolean = other is ZellStdLibrary && other.root == root

        override fun hashCode(): Int = root.hashCode()

        override fun getPresentableText(): String = LIBRARY_NAME

        override fun getLocationString(): String = root.presentableUrl

        override fun getIcon(unused: Boolean): Icon = ZellIcons.FILE
    }

    companion object {
        const val LIBRARY_NAME: String = "Zell std"
    }
}
