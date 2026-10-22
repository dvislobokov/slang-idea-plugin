package org.zell.idea.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.zell.idea.lang.ZellIcons
import javax.swing.Icon

/** Settings | Editor | Color Scheme | Zell. */
class ZellColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon = ZellIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = ZellSyntaxHighlighter()

    override fun getDemoText(): String = DEMO

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = TAGS

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Zell"

    private companion object {
        val TAGS: Map<String, TextAttributesKey> = mapOf(
            "attr" to ZellColors.ATTRIBUTE,
            "type" to ZellColors.TYPE_NAME,
            "iface" to ZellColors.INTERFACE_NAME,
            "fn" to ZellColors.FUNCTION_DECLARATION,
            "const" to ZellColors.CONST_DECLARATION,
            "ns" to ZellColors.NAMESPACE,
        )

        val DESCRIPTORS: Array<AttributesDescriptor> = arrayOf(
            AttributesDescriptor("Keyword", ZellColors.KEYWORD),
            AttributesDescriptor("nil, true, false", ZellColors.CONSTANT),
            AttributesDescriptor("String", ZellColors.STRING),
            AttributesDescriptor("Char", ZellColors.CHAR),
            AttributesDescriptor("Number", ZellColors.NUMBER),
            AttributesDescriptor("Comments//Line comment", ZellColors.LINE_COMMENT),
            AttributesDescriptor("Comments//Block comment", ZellColors.BLOCK_COMMENT),
            AttributesDescriptor("Operators//Operator", ZellColors.OPERATOR),
            AttributesDescriptor("Operators//Error propagation ?", ZellColors.QUESTION),
            AttributesDescriptor("Braces and punctuation//Braces", ZellColors.BRACES),
            AttributesDescriptor("Braces and punctuation//Brackets", ZellColors.BRACKETS),
            AttributesDescriptor("Braces and punctuation//Parentheses", ZellColors.PARENTHESES),
            AttributesDescriptor("Braces and punctuation//Comma", ZellColors.COMMA),
            AttributesDescriptor("Braces and punctuation//Semicolon", ZellColors.SEMICOLON),
            AttributesDescriptor("Braces and punctuation//Dot", ZellColors.DOT),
            AttributesDescriptor("Identifier", ZellColors.IDENTIFIER),
            AttributesDescriptor("Attribute", ZellColors.ATTRIBUTE),
            AttributesDescriptor("Declarations//Type name", ZellColors.TYPE_NAME),
            AttributesDescriptor("Declarations//Interface name", ZellColors.INTERFACE_NAME),
            AttributesDescriptor("Declarations//Function declaration", ZellColors.FUNCTION_DECLARATION),
            AttributesDescriptor("Declarations//Constant declaration", ZellColors.CONST_DECLARATION),
            AttributesDescriptor("Namespace", ZellColors.NAMESPACE),
            AttributesDescriptor("Bad character", ZellColors.BAD_CHARACTER),
        )

        val DEMO = """
            |// A small Zell program
            |namespace <ns>examples.shapes</ns>
            |
            |import <ns>std.io</ns>
            |
            |const <const>Limit</const> = 0x_ff
            |
            |interface <iface>Shape</iface> {
            |    func Area() float
            |}
            |
            |<attr>@json</attr>("point")
            |struct <type>Point</type> implements Shape {
            |    X float
            |    Y float
            |
            |    func <fn>Area</fn>() float {
            |        return 0.0
            |    }
            |}
            |
            |/* block /* nested */ comment */
            |async func <fn>load</fn>(path string) string! {
            |    var text = try io.ReadFile(path) else "none"
            |    var raw = `C:\dir`
            |    var c = 'x'
            |    if text == nil || !true {
            |        return error("empty")
            |    }
            |    var n = parse(text)?
            |    return text
            |}
            |""".trimMargin()
    }
}
