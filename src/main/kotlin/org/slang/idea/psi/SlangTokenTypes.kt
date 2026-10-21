package org.slang.idea.psi

import com.intellij.psi.tree.IElementType

/**
 * Tokens the grammar does not reference in any rule (trivia). They are
 * declared here rather than taken from the generated [SlangTypes] so that
 * the lexer does not depend on whether Grammar-Kit emits unused tokens.
 * Whitespace (including newlines) is [com.intellij.psi.TokenType.WHITE_SPACE].
 */
object SlangTokenTypes {
    /** `// ...` up to the end of the line (also `///` doc comments). */
    @JvmField
    val LINE_COMMENT: IElementType = SlangTokenType("LINE_COMMENT")

    /** `/* ... */`, nestable. */
    @JvmField
    val BLOCK_COMMENT: IElementType = SlangTokenType("BLOCK_COMMENT")
}
