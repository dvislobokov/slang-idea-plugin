package org.slang.idea

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.slang.idea.highlighting.SlangColors
import org.slang.idea.highlighting.SlangSyntaxHighlighter
import org.slang.idea.lexer.SlangHighlightingLexer

/** Lexer-level colours and the syntactic annotator (declaration names, attributes, namespaces). */
class SlangHighlightingTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/highlighting"

    fun testLexerColors() {
        val src = "if x { return \"s\" + `r` + 'c' + 1.5 } // c\n/* b */ nil?"
        val colors = lexerColors(src)
        assertEquals(SlangColors.KEYWORD, colors["if"])
        assertEquals(SlangColors.KEYWORD, colors["return"])
        assertEquals(SlangColors.STRING, colors["\"s\""])
        assertEquals(SlangColors.STRING, colors["`r`"])
        assertEquals(SlangColors.CHAR, colors["'c'"])
        assertEquals(SlangColors.NUMBER, colors["1.5"])
        assertEquals(SlangColors.LINE_COMMENT, colors["// c"])
        assertEquals(SlangColors.BLOCK_COMMENT, colors["/* b */"])
        assertEquals(SlangColors.CONSTANT, colors["nil"])
        assertEquals(SlangColors.QUESTION, colors["?"])
        assertEquals(SlangColors.BRACES, colors["{"])
    }

    fun testAnnotator() {
        myFixture.configureByFile("annotator.sl")
        val infos = myFixture.doHighlighting()
        fun keyOf(text: String): TextAttributesKey? {
            val offset = myFixture.file.text.indexOf(text)
            assertTrue("'$text' not in the fixture", offset >= 0)
            return infos.firstOrNull { it.startOffset == offset && it.endOffset == offset + text.length }?.forcedTextAttributesKey
        }
        assertEquals(SlangColors.NAMESPACE, keyOf("examples.shapes"))
        assertEquals(SlangColors.NAMESPACE, keyOf("std.io"))
        assertEquals(SlangColors.ATTRIBUTE, keyOf("json"))
        assertEquals(SlangColors.TYPE_NAME, keyOf("Point"))
        assertEquals(SlangColors.INTERFACE_NAME, keyOf("Shape"))
        assertEquals(SlangColors.TYPE_NAME, keyOf("Color"))
        assertEquals(SlangColors.FUNCTION_DECLARATION, keyOf("Area"))
        assertEquals(SlangColors.FUNCTION_DECLARATION, keyOf("main"))
        assertEquals(SlangColors.CONST_DECLARATION, keyOf("Limit"))
    }

    private fun lexerColors(src: String): Map<String, TextAttributesKey?> {
        val lexer = SlangHighlightingLexer()
        val highlighter = SlangSyntaxHighlighter()
        val out = HashMap<String, TextAttributesKey?>()
        lexer.start(src, 0, src.length, 0)
        while (lexer.tokenType != null) {
            val text = src.substring(lexer.tokenStart, lexer.tokenEnd)
            out.putIfAbsent(text, highlighter.getTokenHighlights(lexer.tokenType).firstOrNull())
            lexer.advance()
        }
        return out
    }
}
