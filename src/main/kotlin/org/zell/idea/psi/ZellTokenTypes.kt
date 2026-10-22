package org.zell.idea.psi

import com.intellij.psi.tree.IElementType

/**
 * Tokens the grammar does not reference in any rule (trivia). They are
 * declared here rather than taken from the generated [ZellTypes] so that
 * the lexer does not depend on whether Grammar-Kit emits unused tokens.
 * Whitespace (including newlines) is [com.intellij.psi.TokenType.WHITE_SPACE].
 */
object ZellTokenTypes {
    /** `// ...` up to the end of the line (also `///` doc comments). */
    @JvmField
    val LINE_COMMENT: IElementType = ZellTokenType("LINE_COMMENT")

    /** `/* ... */`, nestable. */
    @JvmField
    val BLOCK_COMMENT: IElementType = ZellTokenType("BLOCK_COMMENT")
}
