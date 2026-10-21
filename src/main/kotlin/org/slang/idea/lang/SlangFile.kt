package org.slang.idea.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.slang.idea.psi.SlangTypes

/** PSI root of a `.sl` file. */
class SlangFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SlangLanguage) {
    override fun getFileType(): FileType = SlangFileType

    /** The `namespace a.b.c` of the file, or null while it is missing. */
    val namespaceName: String?
        get() = node.findChildByType(SlangTypes.NAMESPACE_DECL)
            ?.findChildByType(SlangTypes.QUALIFIED_NAME)
            ?.text

    override fun toString(): String = "Slang file"
}
