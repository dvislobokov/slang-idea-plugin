package org.slang.idea.psi

import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet

/** Token groups shared by the parser definition, highlighter and editor features. */
object SlangTokenSets {
    @JvmField
    val WHITESPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

    @JvmField
    val COMMENTS: TokenSet = TokenSet.create(SlangTokenTypes.LINE_COMMENT, SlangTokenTypes.BLOCK_COMMENT)

    @JvmField
    val STRINGS: TokenSet = TokenSet.create(SlangTypes.STRING_LITERAL, SlangTypes.RAW_STRING_LITERAL, SlangTypes.CHAR_LITERAL)

    @JvmField
    val NUMBERS: TokenSet = TokenSet.create(SlangTypes.INT_LITERAL, SlangTypes.FLOAT_LITERAL)

    /** Reserved words of token.rs (`_` is punctuation-like and not included). */
    @JvmField
    val KEYWORDS: TokenSet = TokenSet.create(
        SlangTypes.NAMESPACE, SlangTypes.IMPORT, SlangTypes.AS, SlangTypes.STRUCT,
        SlangTypes.INTERFACE, SlangTypes.ENUM, SlangTypes.FUNC, SlangTypes.STATIC,
        SlangTypes.ASYNC, SlangTypes.AWAIT, SlangTypes.VAR, SlangTypes.CONST,
        SlangTypes.IF, SlangTypes.ELSE, SlangTypes.FOR, SlangTypes.IN,
        SlangTypes.SWITCH, SlangTypes.CASE, SlangTypes.DEFAULT, SlangTypes.RETURN,
        SlangTypes.BREAK, SlangTypes.CONTINUE, SlangTypes.DEFER, SlangTypes.TRY,
        SlangTypes.CATCH, SlangTypes.NEW, SlangTypes.MAP, SlangTypes.THIS,
    )

    /** `nil`, `true`, `false`. */
    @JvmField
    val CONSTANTS: TokenSet = TokenSet.create(SlangTypes.NIL, SlangTypes.TRUE, SlangTypes.FALSE)

    @JvmField
    val OPERATORS: TokenSet = TokenSet.create(
        SlangTypes.DOTDOT, SlangTypes.BANG, SlangTypes.ARROW,
        SlangTypes.AMP, SlangTypes.STAR, SlangTypes.PLUS, SlangTypes.MINUS, SlangTypes.SLASH,
        SlangTypes.PERCENT, SlangTypes.PIPE, SlangTypes.CARET, SlangTypes.SHL, SlangTypes.SHR,
        SlangTypes.ANDAND, SlangTypes.OROR, SlangTypes.LT, SlangTypes.GT, SlangTypes.LE,
        SlangTypes.GE, SlangTypes.EQEQ, SlangTypes.NOTEQ, SlangTypes.ASSIGN, SlangTypes.PLUSEQ,
        SlangTypes.MINUSEQ, SlangTypes.STAREQ, SlangTypes.SLASHEQ, SlangTypes.PERCENTEQ,
        SlangTypes.AMPEQ, SlangTypes.PIPEEQ, SlangTypes.CARETEQ, SlangTypes.SHLEQ,
        SlangTypes.SHREQ, SlangTypes.ATEQ,
    )
}
