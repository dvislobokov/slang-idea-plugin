package org.slang.idea.structure

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.slang.idea.psi.SlangTypes

class SlangBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS = arrayOf(
            BracePair(SlangTypes.LBRACE, SlangTypes.RBRACE, true),
            BracePair(SlangTypes.LPAREN, SlangTypes.RPAREN, false),
            BracePair(SlangTypes.LBRACKET, SlangTypes.RBRACKET, false),
        )
    }
}
