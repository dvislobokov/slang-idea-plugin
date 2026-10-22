package org.slang.idea.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as D
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

/**
 * Text attribute keys, following the categories of the VS Code grammar
 * (editors/vscode/syntaxes/slang.tmLanguage.json). Lexer-level keys come from
 * [SlangSyntaxHighlighter]; declaration names, attributes and namespaces from
 * [SlangHighlightingAnnotator]; slang-lsp semantic tokens refine the rest.
 */
object SlangColors {
    @JvmField val KEYWORD: TextAttributesKey = createTextAttributesKey("SLANG_KEYWORD", D.KEYWORD)
    @JvmField val CONSTANT: TextAttributesKey = createTextAttributesKey("SLANG_CONSTANT", D.KEYWORD)
    @JvmField val STRING: TextAttributesKey = createTextAttributesKey("SLANG_STRING", D.STRING)
    @JvmField val CHAR: TextAttributesKey = createTextAttributesKey("SLANG_CHAR", D.STRING)
    @JvmField val NUMBER: TextAttributesKey = createTextAttributesKey("SLANG_NUMBER", D.NUMBER)
    @JvmField val LINE_COMMENT: TextAttributesKey = createTextAttributesKey("SLANG_LINE_COMMENT", D.LINE_COMMENT)
    @JvmField val BLOCK_COMMENT: TextAttributesKey = createTextAttributesKey("SLANG_BLOCK_COMMENT", D.BLOCK_COMMENT)
    @JvmField val OPERATOR: TextAttributesKey = createTextAttributesKey("SLANG_OPERATOR", D.OPERATION_SIGN)
    /** `?` error propagation, as `keyword.operator.error` in the VS Code grammar. */
    @JvmField val QUESTION: TextAttributesKey = createTextAttributesKey("SLANG_QUESTION", D.KEYWORD)
    @JvmField val BRACES: TextAttributesKey = createTextAttributesKey("SLANG_BRACES", D.BRACES)
    @JvmField val BRACKETS: TextAttributesKey = createTextAttributesKey("SLANG_BRACKETS", D.BRACKETS)
    @JvmField val PARENTHESES: TextAttributesKey = createTextAttributesKey("SLANG_PARENTHESES", D.PARENTHESES)
    @JvmField val COMMA: TextAttributesKey = createTextAttributesKey("SLANG_COMMA", D.COMMA)
    @JvmField val SEMICOLON: TextAttributesKey = createTextAttributesKey("SLANG_SEMICOLON", D.SEMICOLON)
    @JvmField val DOT: TextAttributesKey = createTextAttributesKey("SLANG_DOT", D.DOT)
    @JvmField val IDENTIFIER: TextAttributesKey = createTextAttributesKey("SLANG_IDENTIFIER", D.IDENTIFIER)
    @JvmField val BAD_CHARACTER: TextAttributesKey = createTextAttributesKey("SLANG_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

    // PSI-based (annotator)
    /** `@json`, `@cfg(...)`: the `@` and the attribute path. */
    @JvmField val ATTRIBUTE: TextAttributesKey = createTextAttributesKey("SLANG_ATTRIBUTE", D.METADATA)
    @JvmField val TYPE_NAME: TextAttributesKey = createTextAttributesKey("SLANG_TYPE_NAME", D.CLASS_NAME)
    @JvmField val INTERFACE_NAME: TextAttributesKey = createTextAttributesKey("SLANG_INTERFACE_NAME", D.INTERFACE_NAME)
    @JvmField val FUNCTION_DECLARATION: TextAttributesKey = createTextAttributesKey("SLANG_FUNCTION_DECLARATION", D.FUNCTION_DECLARATION)
    @JvmField val CONST_DECLARATION: TextAttributesKey = createTextAttributesKey("SLANG_CONST_DECLARATION", D.CONSTANT)
    /** The path after `namespace` and `import`. */
    @JvmField val NAMESPACE: TextAttributesKey = createTextAttributesKey("SLANG_NAMESPACE", D.CLASS_REFERENCE)
}
