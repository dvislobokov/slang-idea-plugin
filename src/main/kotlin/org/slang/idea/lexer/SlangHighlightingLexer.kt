package org.slang.idea.lexer

import com.intellij.lexer.FlexAdapter

/**
 * The raw JFlex lexer: every token of the source, no automatic semicolons.
 * Used for syntax highlighting (restartable from any token boundary, all
 * token boundaries are in the initial state) and as the input of [SlangLexer].
 */
class SlangHighlightingLexer : FlexAdapter(_SlangLexer())
