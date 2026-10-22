package org.slang.idea.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.slang.idea.lexer.SlangHighlightingLexer
import org.slang.idea.psi.SlangTokenSets
import org.slang.idea.psi.SlangTokenTypes
import org.slang.idea.psi.SlangTypes

/** Lexer-based highlighting (no semicolon insertion needed here). */
class SlangSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = SlangHighlightingLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        pack(keyFor(tokenType))

    companion object {
        @JvmStatic
        fun keyFor(type: IElementType?): TextAttributesKey? = when {
            type == null -> null
            type == SlangTokenTypes.LINE_COMMENT -> SlangColors.LINE_COMMENT
            type == SlangTokenTypes.BLOCK_COMMENT -> SlangColors.BLOCK_COMMENT
            type == SlangTypes.CHAR_LITERAL -> SlangColors.CHAR
            type == SlangTypes.STRING_LITERAL || type == SlangTypes.RAW_STRING_LITERAL -> SlangColors.STRING
            SlangTokenSets.NUMBERS.contains(type) -> SlangColors.NUMBER
            SlangTokenSets.KEYWORDS.contains(type) -> SlangColors.KEYWORD
            SlangTokenSets.CONSTANTS.contains(type) -> SlangColors.CONSTANT
            type == SlangTypes.QUESTION -> SlangColors.QUESTION
            type == SlangTypes.AT || type == SlangTypes.ATEQ -> SlangColors.OPERATOR
            SlangTokenSets.OPERATORS.contains(type) -> SlangColors.OPERATOR
            type == SlangTypes.LBRACE || type == SlangTypes.RBRACE -> SlangColors.BRACES
            type == SlangTypes.LBRACKET || type == SlangTypes.RBRACKET -> SlangColors.BRACKETS
            type == SlangTypes.LPAREN || type == SlangTypes.RPAREN -> SlangColors.PARENTHESES
            type == SlangTypes.COMMA -> SlangColors.COMMA
            type == SlangTypes.SEMI -> SlangColors.SEMICOLON
            type == SlangTypes.DOT -> SlangColors.DOT
            type == SlangTypes.COLON -> SlangColors.OPERATOR
            type == SlangTypes.IDENTIFIER || type == SlangTypes.UNDERSCORE -> SlangColors.IDENTIFIER
            type == TokenType.BAD_CHARACTER -> SlangColors.BAD_CHARACTER
            else -> null
        }
    }
}

class SlangSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        SlangSyntaxHighlighter()
}
