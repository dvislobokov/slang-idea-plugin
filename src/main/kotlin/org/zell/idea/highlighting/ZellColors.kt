package org.zell.idea.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as D
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

/**
 * Text attribute keys, following the categories of the VS Code grammar
 * (editors/vscode/syntaxes/zell.tmLanguage.json). Lexer-level keys come from
 * [ZellSyntaxHighlighter]; declaration names, attributes and namespaces from
 * [ZellHighlightingAnnotator]; zell-lsp semantic tokens refine the rest.
 */
object ZellColors {
    @JvmField val KEYWORD: TextAttributesKey = createTextAttributesKey("ZELL_KEYWORD", D.KEYWORD)
    @JvmField val CONSTANT: TextAttributesKey = createTextAttributesKey("ZELL_CONSTANT", D.KEYWORD)
    @JvmField val STRING: TextAttributesKey = createTextAttributesKey("ZELL_STRING", D.STRING)
    @JvmField val CHAR: TextAttributesKey = createTextAttributesKey("ZELL_CHAR", D.STRING)
    @JvmField val NUMBER: TextAttributesKey = createTextAttributesKey("ZELL_NUMBER", D.NUMBER)
    @JvmField val LINE_COMMENT: TextAttributesKey = createTextAttributesKey("ZELL_LINE_COMMENT", D.LINE_COMMENT)
    @JvmField val BLOCK_COMMENT: TextAttributesKey = createTextAttributesKey("ZELL_BLOCK_COMMENT", D.BLOCK_COMMENT)
    @JvmField val OPERATOR: TextAttributesKey = createTextAttributesKey("ZELL_OPERATOR", D.OPERATION_SIGN)
    /** `?` error propagation, as `keyword.operator.error` in the VS Code grammar. */
    @JvmField val QUESTION: TextAttributesKey = createTextAttributesKey("ZELL_QUESTION", D.KEYWORD)
    @JvmField val BRACES: TextAttributesKey = createTextAttributesKey("ZELL_BRACES", D.BRACES)
    @JvmField val BRACKETS: TextAttributesKey = createTextAttributesKey("ZELL_BRACKETS", D.BRACKETS)
    @JvmField val PARENTHESES: TextAttributesKey = createTextAttributesKey("ZELL_PARENTHESES", D.PARENTHESES)
    @JvmField val COMMA: TextAttributesKey = createTextAttributesKey("ZELL_COMMA", D.COMMA)
    @JvmField val SEMICOLON: TextAttributesKey = createTextAttributesKey("ZELL_SEMICOLON", D.SEMICOLON)
    @JvmField val DOT: TextAttributesKey = createTextAttributesKey("ZELL_DOT", D.DOT)
    @JvmField val IDENTIFIER: TextAttributesKey = createTextAttributesKey("ZELL_IDENTIFIER", D.IDENTIFIER)
    @JvmField val BAD_CHARACTER: TextAttributesKey = createTextAttributesKey("ZELL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

    // PSI-based (annotator)
    /** `@json`, `@cfg(...)`: the `@` and the attribute path. */
    @JvmField val ATTRIBUTE: TextAttributesKey = createTextAttributesKey("ZELL_ATTRIBUTE", D.METADATA)
    @JvmField val TYPE_NAME: TextAttributesKey = createTextAttributesKey("ZELL_TYPE_NAME", D.CLASS_NAME)
    @JvmField val INTERFACE_NAME: TextAttributesKey = createTextAttributesKey("ZELL_INTERFACE_NAME", D.INTERFACE_NAME)
    @JvmField val FUNCTION_DECLARATION: TextAttributesKey = createTextAttributesKey("ZELL_FUNCTION_DECLARATION", D.FUNCTION_DECLARATION)
    @JvmField val CONST_DECLARATION: TextAttributesKey = createTextAttributesKey("ZELL_CONST_DECLARATION", D.CONSTANT)
    /** The path after `namespace` and `import`. */
    @JvmField val NAMESPACE: TextAttributesKey = createTextAttributesKey("ZELL_NAMESPACE", D.CLASS_REFERENCE)
}
