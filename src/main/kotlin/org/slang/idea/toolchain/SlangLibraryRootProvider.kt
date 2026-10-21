package org.slang.idea.toolchain

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
import org.slang.idea.lang.SlangIcons
import javax.swing.Icon

/**
 * Attaches the standard library sources ([SlangToolchain.stdRoot], i.e.
 * `slang env SLANG_STD`) as an external library, like GOROOT in the Go
 * plugin: PSI features (structure, Go to File/Symbol) see std; semantic
 * navigation into std comes from slang-lsp.
 */
class SlangLibraryRootProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> =
        listOfNotNull(stdRoot()?.let(::SlangStdLibrary))

    override fun getRootsToWatch(project: Project): Collection<VirtualFile> = listOfNotNull(stdRoot())

    private fun stdRoot(): VirtualFile? =
        SlangToolchain.stdRoot()?.let { LocalFileSystem.getInstance().findFileByNioFile(it) }

    /** Re-indexes std after the toolchain settings change. */
    class ToolchainListener : SlangToolchain.Listener {
        override fun toolchainChanged() {
            val app = ApplicationManager.getApplication()
            app.executeOnPooledThread {
                val newRoot = SlangToolchain.stdRoot()?.let { LocalFileSystem.getInstance().refreshAndFindFileByNioFile(it) }
                app.invokeLater {
                    WriteAction.run<RuntimeException> {
                        for (project in ProjectManager.getInstance().openProjects) {
                            if (project.isDisposed) continue
                            AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(
                                project, LIBRARY_NAME, emptyList(), listOfNotNull(newRoot), "Slang toolchain change",
                            )
                        }
                    }
                }
            }
        }
    }

    private class SlangStdLibrary(private val root: VirtualFile) : SyntheticLibrary(), ItemPresentation {
        override fun getSourceRoots(): Collection<VirtualFile> = listOf(root)

        override fun equals(other: Any?): Boolean = other is SlangStdLibrary && other.root == root

        override fun hashCode(): Int = root.hashCode()

        override fun getPresentableText(): String = LIBRARY_NAME

        override fun getLocationString(): String = root.presentableUrl

        override fun getIcon(unused: Boolean): Icon = SlangIcons.FILE
    }

    companion object {
        const val LIBRARY_NAME: String = "Slang std"
    }
}
