package org.zell.idea.psi

import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet

/** Token groups shared by the parser definition, highlighter and editor features. */
object ZellTokenSets {
    @JvmField
    val WHITESPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

    @JvmField
    val COMMENTS: TokenSet = TokenSet.create(ZellTokenTypes.LINE_COMMENT, ZellTokenTypes.BLOCK_COMMENT)

    @JvmField
    val STRINGS: TokenSet = TokenSet.create(ZellTypes.STRING_LITERAL, ZellTypes.RAW_STRING_LITERAL, ZellTypes.CHAR_LITERAL)

    @JvmField
    val NUMBERS: TokenSet = TokenSet.create(ZellTypes.INT_LITERAL, ZellTypes.FLOAT_LITERAL)

    /** Reserved words of token.rs (`_` is punctuation-like and not included). */
    @JvmField
    val KEYWORDS: TokenSet = TokenSet.create(
        ZellTypes.NAMESPACE, ZellTypes.IMPORT, ZellTypes.AS, ZellTypes.STRUCT,
        ZellTypes.INTERFACE, ZellTypes.ENUM, ZellTypes.FUNC, ZellTypes.STATIC,
        ZellTypes.ASYNC, ZellTypes.AWAIT, ZellTypes.VAR, ZellTypes.CONST,
        ZellTypes.IF, ZellTypes.ELSE, ZellTypes.FOR, ZellTypes.IN,
        ZellTypes.SWITCH, ZellTypes.CASE, ZellTypes.DEFAULT, ZellTypes.RETURN,
        ZellTypes.BREAK, ZellTypes.CONTINUE, ZellTypes.DEFER, ZellTypes.TRY,
        ZellTypes.CATCH, ZellTypes.NEW, ZellTypes.MAP, ZellTypes.THIS,
    )

    /** `nil`, `true`, `false`. */
    @JvmField
    val CONSTANTS: TokenSet = TokenSet.create(ZellTypes.NIL, ZellTypes.TRUE, ZellTypes.FALSE)

    @JvmField
    val OPERATORS: TokenSet = TokenSet.create(
        ZellTypes.DOTDOT, ZellTypes.BANG, ZellTypes.ARROW,
        ZellTypes.AMP, ZellTypes.STAR, ZellTypes.PLUS, ZellTypes.MINUS, ZellTypes.SLASH,
        ZellTypes.PERCENT, ZellTypes.PIPE, ZellTypes.CARET, ZellTypes.SHL, ZellTypes.SHR,
        ZellTypes.ANDAND, ZellTypes.OROR, ZellTypes.LT, ZellTypes.GT, ZellTypes.LE,
        ZellTypes.GE, ZellTypes.EQEQ, ZellTypes.NOTEQ, ZellTypes.ASSIGN, ZellTypes.PLUSEQ,
        ZellTypes.MINUSEQ, ZellTypes.STAREQ, ZellTypes.SLASHEQ, ZellTypes.PERCENTEQ,
        ZellTypes.AMPEQ, ZellTypes.PIPEEQ, ZellTypes.CARETEQ, ZellTypes.SHLEQ,
        ZellTypes.SHREQ, ZellTypes.ATEQ,
    )
}
