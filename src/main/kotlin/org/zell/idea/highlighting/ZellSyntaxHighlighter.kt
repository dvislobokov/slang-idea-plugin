package org.zell.idea.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.zell.idea.lexer.ZellHighlightingLexer
import org.zell.idea.psi.ZellTokenSets
import org.zell.idea.psi.ZellTokenTypes
import org.zell.idea.psi.ZellTypes

/** Lexer-based highlighting (no semicolon insertion needed here). */
class ZellSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = ZellHighlightingLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        pack(keyFor(tokenType))

    companion object {
        @JvmStatic
        fun keyFor(type: IElementType?): TextAttributesKey? = when {
            type == null -> null
            type == ZellTokenTypes.LINE_COMMENT -> ZellColors.LINE_COMMENT
            type == ZellTokenTypes.BLOCK_COMMENT -> ZellColors.BLOCK_COMMENT
            type == ZellTypes.CHAR_LITERAL -> ZellColors.CHAR
            type == ZellTypes.STRING_LITERAL || type == ZellTypes.RAW_STRING_LITERAL -> ZellColors.STRING
            ZellTokenSets.NUMBERS.contains(type) -> ZellColors.NUMBER
            ZellTokenSets.KEYWORDS.contains(type) -> ZellColors.KEYWORD
            ZellTokenSets.CONSTANTS.contains(type) -> ZellColors.CONSTANT
            type == ZellTypes.QUESTION -> ZellColors.QUESTION
            type == ZellTypes.AT || type == ZellTypes.ATEQ -> ZellColors.OPERATOR
            ZellTokenSets.OPERATORS.contains(type) -> ZellColors.OPERATOR
            type == ZellTypes.LBRACE || type == ZellTypes.RBRACE -> ZellColors.BRACES
            type == ZellTypes.LBRACKET || type == ZellTypes.RBRACKET -> ZellColors.BRACKETS
            type == ZellTypes.LPAREN || type == ZellTypes.RPAREN -> ZellColors.PARENTHESES
            type == ZellTypes.COMMA -> ZellColors.COMMA
            type == ZellTypes.SEMI -> ZellColors.SEMICOLON
            type == ZellTypes.DOT -> ZellColors.DOT
            type == ZellTypes.COLON -> ZellColors.OPERATOR
            type == ZellTypes.IDENTIFIER || type == ZellTypes.UNDERSCORE -> ZellColors.IDENTIFIER
            type == TokenType.BAD_CHARACTER -> ZellColors.BAD_CHARACTER
            else -> null
        }
    }
}

class ZellSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        ZellSyntaxHighlighter()
}
