package org.slang.idea.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.slang.idea.lang.SlangIcons
import javax.swing.Icon

/** Settings | Editor | Color Scheme | Slang. */
class SlangColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon = SlangIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = SlangSyntaxHighlighter()

    override fun getDemoText(): String = DEMO

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = TAGS

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Slang"

    private companion object {
        val TAGS: Map<String, TextAttributesKey> = mapOf(
            "attr" to SlangColors.ATTRIBUTE,
            "type" to SlangColors.TYPE_NAME,
            "iface" to SlangColors.INTERFACE_NAME,
            "fn" to SlangColors.FUNCTION_DECLARATION,
            "const" to SlangColors.CONST_DECLARATION,
            "ns" to SlangColors.NAMESPACE,
        )

        val DESCRIPTORS: Array<AttributesDescriptor> = arrayOf(
            AttributesDescriptor("Keyword", SlangColors.KEYWORD),
            AttributesDescriptor("nil, true, false", SlangColors.CONSTANT),
            AttributesDescriptor("String", SlangColors.STRING),
            AttributesDescriptor("Char", SlangColors.CHAR),
            AttributesDescriptor("Number", SlangColors.NUMBER),
            AttributesDescriptor("Comments//Line comment", SlangColors.LINE_COMMENT),
            AttributesDescriptor("Comments//Block comment", SlangColors.BLOCK_COMMENT),
            AttributesDescriptor("Operators//Operator", SlangColors.OPERATOR),
            AttributesDescriptor("Operators//Error propagation ?", SlangColors.QUESTION),
            AttributesDescriptor("Braces and punctuation//Braces", SlangColors.BRACES),
            AttributesDescriptor("Braces and punctuation//Brackets", SlangColors.BRACKETS),
            AttributesDescriptor("Braces and punctuation//Parentheses", SlangColors.PARENTHESES),
            AttributesDescriptor("Braces and punctuation//Comma", SlangColors.COMMA),
            AttributesDescriptor("Braces and punctuation//Semicolon", SlangColors.SEMICOLON),
            AttributesDescriptor("Braces and punctuation//Dot", SlangColors.DOT),
            AttributesDescriptor("Identifier", SlangColors.IDENTIFIER),
            AttributesDescriptor("Attribute", SlangColors.ATTRIBUTE),
            AttributesDescriptor("Declarations//Type name", SlangColors.TYPE_NAME),
            AttributesDescriptor("Declarations//Interface name", SlangColors.INTERFACE_NAME),
            AttributesDescriptor("Declarations//Function declaration", SlangColors.FUNCTION_DECLARATION),
            AttributesDescriptor("Declarations//Constant declaration", SlangColors.CONST_DECLARATION),
            AttributesDescriptor("Namespace", SlangColors.NAMESPACE),
            AttributesDescriptor("Bad character", SlangColors.BAD_CHARACTER),
        )

        val DEMO = """
            |// A small Slang program
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
