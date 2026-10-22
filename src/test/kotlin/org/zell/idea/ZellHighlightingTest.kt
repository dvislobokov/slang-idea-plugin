package org.zell.idea

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.zell.idea.highlighting.ZellColors
import org.zell.idea.highlighting.ZellSyntaxHighlighter
import org.zell.idea.lexer.ZellHighlightingLexer

/** Lexer-level colours and the syntactic annotator (declaration names, attributes, namespaces). */
class ZellHighlightingTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/highlighting"

    fun testLexerColors() {
        val src = "if x { return \"s\" + `r` + 'c' + 1.5 } // c\n/* b */ nil?"
        val colors = lexerColors(src)
        assertEquals(ZellColors.KEYWORD, colors["if"])
        assertEquals(ZellColors.KEYWORD, colors["return"])
        assertEquals(ZellColors.STRING, colors["\"s\""])
        assertEquals(ZellColors.STRING, colors["`r`"])
        assertEquals(ZellColors.CHAR, colors["'c'"])
        assertEquals(ZellColors.NUMBER, colors["1.5"])
        assertEquals(ZellColors.LINE_COMMENT, colors["// c"])
        assertEquals(ZellColors.BLOCK_COMMENT, colors["/* b */"])
        assertEquals(ZellColors.CONSTANT, colors["nil"])
        assertEquals(ZellColors.QUESTION, colors["?"])
        assertEquals(ZellColors.BRACES, colors["{"])
    }

    fun testAnnotator() {
        myFixture.configureByFile("annotator.zl")
        val infos = myFixture.doHighlighting()
        fun keyOf(text: String): TextAttributesKey? {
            val offset = myFixture.file.text.indexOf(text)
            assertTrue("'$text' not in the fixture", offset >= 0)
            return infos.firstOrNull { it.startOffset == offset && it.endOffset == offset + text.length }?.forcedTextAttributesKey
        }
        assertEquals(ZellColors.NAMESPACE, keyOf("examples.shapes"))
        assertEquals(ZellColors.NAMESPACE, keyOf("std.io"))
        assertEquals(ZellColors.ATTRIBUTE, keyOf("json"))
        assertEquals(ZellColors.TYPE_NAME, keyOf("Point"))
        assertEquals(ZellColors.INTERFACE_NAME, keyOf("Shape"))
        assertEquals(ZellColors.TYPE_NAME, keyOf("Color"))
        assertEquals(ZellColors.FUNCTION_DECLARATION, keyOf("Area"))
        assertEquals(ZellColors.FUNCTION_DECLARATION, keyOf("main"))
        assertEquals(ZellColors.CONST_DECLARATION, keyOf("Limit"))
    }

    private fun lexerColors(src: String): Map<String, TextAttributesKey?> {
        val lexer = ZellHighlightingLexer()
        val highlighter = ZellSyntaxHighlighter()
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
