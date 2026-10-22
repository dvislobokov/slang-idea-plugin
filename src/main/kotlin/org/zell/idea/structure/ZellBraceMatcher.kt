package org.zell.idea.structure

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.zell.idea.psi.ZellTypes

class ZellBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS = arrayOf(
            BracePair(ZellTypes.LBRACE, ZellTypes.RBRACE, true),
            BracePair(ZellTypes.LPAREN, ZellTypes.RPAREN, false),
            BracePair(ZellTypes.LBRACKET, ZellTypes.RBRACKET, false),
        )
    }
}
