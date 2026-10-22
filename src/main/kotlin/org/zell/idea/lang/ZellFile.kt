package org.zell.idea.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.zell.idea.psi.ZellTypes

/** PSI root of a `.zl` file. */
class ZellFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ZellLanguage) {
    override fun getFileType(): FileType = ZellFileType

    /** The `namespace a.b.c` of the file, or null while it is missing. */
    val namespaceName: String?
        get() = node.findChildByType(ZellTypes.NAMESPACE_DECL)
            ?.findChildByType(ZellTypes.QUALIFIED_NAME)
            ?.text

    override fun toString(): String = "Zell file"
}
