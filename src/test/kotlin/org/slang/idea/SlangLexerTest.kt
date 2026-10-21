package org.slang.idea

import com.intellij.lexer.Lexer
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slang.idea.lexer.SlangHighlightingLexer
import org.slang.idea.lexer.SlangLexer
import org.slang.idea.psi.SlangTokenTypes
import org.slang.idea.psi.SlangTypes
import java.io.File

/**
 * The parser lexer against the compiler's token stream (cases mirror the
 * unit tests of compiler/src/lexer.rs). Plain JUnit: needs the platform jars
 * on the classpath but no IDE application.
 */
class SlangLexerTest {

    // ----- tokens --------------------------------------------------------

    @Test
    fun keywordsAndIdentifiers() = check(
        "namespace app import as func x _ _y привет",
        "NAMESPACE IDENTIFIER IMPORT AS FUNC IDENTIFIER UNDERSCORE IDENTIFIER IDENTIFIER SEMI",
    )

    @Test
    fun allKeywords() = check(
        "namespace import as struct interface enum func static async await var const if else for in " +
            "switch case default return break continue defer try catch new nil true false map this _",
        "NAMESPACE IMPORT AS STRUCT INTERFACE ENUM FUNC STATIC ASYNC AWAIT VAR CONST IF ELSE FOR IN " +
            "SWITCH CASE DEFAULT RETURN BREAK CONTINUE DEFER TRY CATCH NEW NIL TRUE FALSE MAP THIS UNDERSCORE SEMI",
    )

    @Test
    fun contextualKeywordsAreIdentifiers() = check(
        "extern implements sealed weak namespaces",
        "IDENTIFIER IDENTIFIER IDENTIFIER IDENTIFIER IDENTIFIER SEMI",
    )

    @Test
    fun integers() = check(
        "123 1_000 0xff 0XFF 0b1010 0o17 0x_ff",
        "INT_LITERAL INT_LITERAL INT_LITERAL INT_LITERAL INT_LITERAL INT_LITERAL INT_LITERAL SEMI",
    )

    @Test
    fun floats() = check(
        "1.5 2e10 3.0e-2 1E+3 1_0.5",
        "FLOAT_LITERAL FLOAT_LITERAL FLOAT_LITERAL FLOAT_LITERAL FLOAT_LITERAL SEMI",
    )

    @Test
    fun rangesAndTupleFields() {
        check("0..10", "INT_LITERAL DOTDOT INT_LITERAL SEMI")
        check("t.0", "IDENTIFIER DOT INT_LITERAL SEMI")
        check("s[1..]", "IDENTIFIER LBRACKET INT_LITERAL DOTDOT RBRACKET SEMI")
    }

    @Test
    fun stringsAndEscapes() {
        val src = "\"a\\nb\\t\\\\\\\"\\r\\0\\u{41}\\u{1F600}\""
        check(src, "STRING_LITERAL SEMI")
        assertEquals(src, texts(src).first())
        check("\"привет\" \"{0} and {1}\"", "STRING_LITERAL STRING_LITERAL SEMI")
        // An escaped backslash does not escape the closing quote.
        check("f(\"\\\\\") || g(\"\\\\\", \"/\")", "IDENTIFIER LPAREN STRING_LITERAL RPAREN OROR IDENTIFIER LPAREN STRING_LITERAL COMMA STRING_LITERAL RPAREN SEMI")
        // Unterminated: the literal ends at the line end and still ends the statement.
        check("x = \"abc\ny", "IDENTIFIER ASSIGN STRING_LITERAL SEMI IDENTIFIER SEMI")
    }

    @Test
    fun rawStringsSpanLines() {
        check("`C:\\dir\\n`", "RAW_STRING_LITERAL SEMI")
        check("x = `\n`\ny", "IDENTIFIER ASSIGN RAW_STRING_LITERAL SEMI IDENTIFIER SEMI")
        check("x = `a\nb\nc`", "IDENTIFIER ASSIGN RAW_STRING_LITERAL SEMI")
    }

    @Test
    fun chars() = check(
        "'a' '\\n' '\\'' '\\u{263A}' 'я'",
        "CHAR_LITERAL CHAR_LITERAL CHAR_LITERAL CHAR_LITERAL CHAR_LITERAL SEMI",
    )

    @Test
    fun punctuationForTheParser() = check(
        "( ) { } [ ] , . .. : ; ? @ ! => & * + - / % | ^ << >> && || < > <= >= == != = += -= *= /= %= &= |= ^= <<= >>= @=",
        "LPAREN RPAREN LBRACE RBRACE LBRACKET RBRACKET COMMA DOT DOTDOT COLON SEMI " +
            "QUESTION AT BANG ARROW AMP STAR PLUS MINUS SLASH PERCENT PIPE CARET SHL GT GT " +
            "ANDAND OROR LT GT LE GE EQEQ NOTEQ ASSIGN PLUSEQ MINUSEQ STAREQ SLASHEQ " +
            "PERCENTEQ AMPEQ PIPEEQ CARETEQ SHLEQ GT GE ATEQ",
    )

    @Test
    fun highlightingLexerKeepsShiftOperators() {
        assertEquals("SHR SHREQ", names(SlangHighlightingLexer(), ">> >>=", significantOnly = true))
    }

    @Test
    fun comments() {
        check("a // comment\nb", "IDENTIFIER SEMI IDENTIFIER SEMI")
        check("a /* x */ b", "IDENTIFIER IDENTIFIER SEMI")
        check("a /* x\n y */ b", "IDENTIFIER SEMI IDENTIFIER SEMI")
        check("a /* /* nested */ */ b", "IDENTIFIER IDENTIFIER SEMI")
        assertEquals(
            "IDENTIFIER WS BLOCK_COMMENT WS IDENTIFIER SEMI",
            names(SlangLexer(), "a /* /* nested */ */ b", significantOnly = false),
        )
    }

    @Test
    fun unterminatedBlockCommentRunsToTheEnd() = assertEquals(
        "IDENTIFIER SEMI WS BLOCK_COMMENT",
        names(SlangLexer(), "a /* never closed", significantOnly = false),
    )

    // ----- automatic semicolons -------------------------------------------

    @Test
    fun semicolonInsertion() = check(
        "var x = 1\nx +\n2\nreturn\n}\n",
        "VAR IDENTIFIER ASSIGN INT_LITERAL SEMI IDENTIFIER PLUS INT_LITERAL SEMI RETURN SEMI RBRACE SEMI",
    )

    @Test
    fun noSemicolonAfterOpenBraceOrComma() =
        check("f(a,\nb)\n{", "IDENTIFIER LPAREN IDENTIFIER COMMA IDENTIFIER RPAREN SEMI LBRACE")

    @Test
    fun newlinesInsideParenthesesAreIgnored() =
        check("f(\na\n)\n", "IDENTIFIER LPAREN IDENTIFIER RPAREN SEMI")

    @Test
    fun bracesInsideParenthesesRestoreTheRule() = check(
        "f(func() {\na\n})",
        "IDENTIFIER LPAREN FUNC LPAREN RPAREN LBRACE IDENTIFIER SEMI RBRACE RPAREN SEMI",
    )

    @Test
    fun bracketsNestWithBraces() = check(
        "[\n{\nx\n}\n]\n",
        "LBRACKET LBRACE IDENTIFIER SEMI RBRACE RBRACKET SEMI",
    )

    @Test
    fun keywordAfterDotIsAName() =
        check("a.async\nb", "IDENTIFIER DOT ASYNC SEMI IDENTIFIER SEMI")

    @Test
    fun fallibleTypesAndQuestionEndStatements() =
        check("int!\nx?\n", "IDENTIFIER BANG SEMI IDENTIFIER QUESTION SEMI")

    @Test
    fun dotAtLineEndJoinsTheNextLine() =
        check("x.\ny\n", "IDENTIFIER DOT IDENTIFIER SEMI")

    @Test
    fun nestedGenericArgumentsClose() = check(
        "var g List<List<int>>\nvar m = Map<string, List<string>>{}\n",
        "VAR IDENTIFIER IDENTIFIER LT IDENTIFIER LT IDENTIFIER GT GT SEMI " +
            "VAR IDENTIFIER ASSIGN IDENTIFIER LT IDENTIFIER COMMA IDENTIFIER LT IDENTIFIER GT GT LBRACE RBRACE SEMI",
    )

    @Test
    fun shiftOperatorsAreSplitForTheParser() {
        check("x >>= 2\ny >> 1\n", "IDENTIFIER GT GE INT_LITERAL SEMI IDENTIFIER GT GT INT_LITERAL SEMI")
        // `>>` at the end of a line ends the statement like `>` (compiler: Shr).
        val toks = tokens(SlangLexer(), "a>>\n")
        assertEquals(listOf(Tok("IDENTIFIER", 0, 1), Tok("GT", 1, 2), Tok("GT", 2, 3), Tok("SEMI", 3, 3), Tok("WS", 3, 4)), toks)
    }

    @Test
    fun explicitSemicolons() = check("a; b", "IDENTIFIER SEMI IDENTIFIER SEMI")

    @Test
    fun unknownCharactersDoNotEndStatements() =
        check("x = $\n1 #", "IDENTIFIER ASSIGN BAD_CHARACTER INT_LITERAL SEMI BAD_CHARACTER")

    @Test
    fun semicolonSpansAreZeroWidthAfterTheToken() {
        // compiler: spans (0,3) (4,6) (7,8) (9,12) (12,12)
        val toks = tokens(SlangLexer(), "var xy = \"s\"\n").filter { it.name != "WS" }
        assertEquals(
            listOf(Tok("VAR", 0, 3), Tok("IDENTIFIER", 4, 6), Tok("ASSIGN", 7, 8), Tok("STRING_LITERAL", 9, 12), Tok("SEMI", 12, 12)),
            toks,
        )
    }

    @Test
    fun semicolonGoesBeforeATrailingComment() = assertEquals(
        listOf(
            Tok("IDENTIFIER", 0, 1), Tok("SEMI", 1, 1), Tok("WS", 1, 2), Tok("LINE_COMMENT", 2, 6),
            Tok("WS", 6, 7), Tok("IDENTIFIER", 7, 8), Tok("SEMI", 8, 8),
        ),
        tokens(SlangLexer(), "a // c\nb"),
    )

    @Test
    fun emptyInput() {
        check("", "")
        check("\n\n  // only a comment", "")
    }

    @Test
    fun helloExample() {
        val src = File("src/test/testData/lexer/hello.sl").readText()
        check(
            src,
            "NAMESPACE IDENTIFIER DOT IDENTIFIER SEMI " +
                "IMPORT IDENTIFIER DOT IDENTIFIER SEMI " +
                "FUNC IDENTIFIER LPAREN RPAREN LBRACE " +
                "IDENTIFIER LPAREN STRING_LITERAL RPAREN SEMI " +
                "RBRACE SEMI",
        )
    }

    @Test
    fun tokensCoverTheInput() {
        val samples = listOf(
            File("src/test/testData/lexer/hello.sl").readText(),
            "a /* x\n y */ b // c\n`raw\n` 'c' \"s\" 1.5e3 >>= >> $",
            "func f() int! {\n\treturn 0\n}\n",
        )
        for (src in samples) {
            for (lexer in listOf(SlangLexer(), SlangHighlightingLexer())) {
                var pos = 0
                for (t in tokens(lexer, src)) {
                    assertEquals("gap or overlap at ${t.start} in ${lexer.javaClass.simpleName}", pos, t.start)
                    assertTrue(t.end >= t.start)
                    pos = t.end
                }
                assertEquals(src.length, pos)
            }
        }
    }

    // ----- helpers -------------------------------------------------------

    private data class Tok(val name: String, val start: Int, val end: Int)

    private fun check(src: String, expected: String) =
        assertEquals("tokens of <$src>", expected, names(SlangLexer(), src, significantOnly = true))

    private fun names(lexer: Lexer, src: String, significantOnly: Boolean): String =
        tokens(lexer, src)
            .filter { !significantOnly || it.name !in TRIVIA }
            .joinToString(" ") { it.name }

    private fun texts(src: String): List<String> =
        tokens(SlangLexer(), src).filter { it.name !in TRIVIA }.map { src.substring(it.start, it.end) }

    private fun tokens(lexer: Lexer, src: String): List<Tok> {
        lexer.start(src, 0, src.length, 0)
        val out = ArrayList<Tok>()
        while (true) {
            val type = lexer.tokenType ?: break
            out.add(Tok(nameOf(type), lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        return out
    }

    companion object {
        private val TRIVIA = setOf("WS", "LINE_COMMENT", "BLOCK_COMMENT")

        /** Constant name in [SlangTypes] for each token type (identity map). */
        private val NAMES: Map<IElementType, String> by lazy {
            val m = HashMap<IElementType, String>()
            for (f in SlangTypes::class.java.fields) {
                val v = f.get(null)
                if (v is IElementType) m[v] = f.name
            }
            m[TokenType.WHITE_SPACE] = "WS"
            m[TokenType.BAD_CHARACTER] = "BAD_CHARACTER"
            m[SlangTokenTypes.LINE_COMMENT] = "LINE_COMMENT"
            m[SlangTokenTypes.BLOCK_COMMENT] = "BLOCK_COMMENT"
            m
        }

        private fun nameOf(type: IElementType): String = NAMES[type] ?: "?$type"
    }
}
