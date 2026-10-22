package org.slang.idea.stubs

import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IStubFileElementType
import org.slang.idea.lang.SlangLanguage

/** Element type factories referenced from the grammar header. */
object SlangStubElementTypes {
    /** `elementTypeFactory` of the declaration rules (see tools/sync-slang.sh). */
    @JvmStatic
    fun factory(name: String): IElementType = SlangDeclStubElementType(name)

    /** File element type; bump the version whenever the stub format or the grammar changes. */
    @JvmField
    val FILE: IStubFileElementType<PsiFileStub<PsiFile>> = object : IStubFileElementType<PsiFileStub<PsiFile>>("SLANG_FILE", SlangLanguage) {
        override fun getStubVersion(): Int = 1

        override fun getExternalId(): String = "slang.FILE"
    }
}
