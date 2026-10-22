package org.zell.idea.stubs

import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IStubFileElementType
import org.zell.idea.lang.ZellLanguage

/** Element type factories referenced from the grammar header. */
object ZellStubElementTypes {
    /** `elementTypeFactory` of the declaration rules (see tools/sync-zell.sh). */
    @JvmStatic
    fun factory(name: String): IElementType = ZellDeclStubElementType(name)

    /** File element type; bump the version whenever the stub format or the grammar changes. */
    @JvmField
    val FILE: IStubFileElementType<PsiFileStub<PsiFile>> = object : IStubFileElementType<PsiFileStub<PsiFile>>("ZELL_FILE", ZellLanguage) {
        override fun getStubVersion(): Int = 1

        override fun getExternalId(): String = "zell.FILE"
    }
}
