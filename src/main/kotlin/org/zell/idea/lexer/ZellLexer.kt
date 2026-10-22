package org.slang.idea.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.slang.idea.psi.SlangTokenTypes
import org.slang.idea.psi.SlangTypes

/**
 * The parser lexer: the JFlex tokens plus Go-style automatic semicolons,
 * mirroring `compiler/src/lexer.rs` of the slang repository exactly:
 *
 * - at a newline (in whitespace or inside a multi-line block comment) and at
 *   the end of the input, a [SlangTypes.SEMI] is inserted when the previous
 *   significant token ends a statement ([endsStatement], = `TokenKind::ends_statement`)
 *   or is a keyword directly after `.` (`namespace examples.async`);
 * - nothing is inserted while the innermost open delimiter is `(` or `[`
 *   (`{` restores the rule);
 * - the inserted semicolon is zero-width and sits right after the token that
 *   ends the statement (before any trailing comment), like the compiler's span.
 *
 * In addition, `>>` is split into two `>` tokens and `>>=` into `>` `>=`, so
 * that nested generic arguments close (`List<List<int>>`) the way the
 * compiler's `expect_gt` splits them; `SlangParserUtil` glues adjacent pairs
 * back into `SHR`/`SHREQ` where the grammar expects an operator.
 *
 * The whole input is tokenized in [start]; the lexer is used only by the
 * parser (always from the initial state), so [getState] is always 0.
 * Highlighting uses [SlangHighlightingLexer].
 */
class SlangLexer : LexerBase() {
    private val flex = SlangHighlightingLexer()

    private var buffer: CharSequence = ""
    private var bufferEnd = 0

    private var types = arrayOfNulls<IElementType>(0)
    private var starts = IntArray(0)
    private var ends = IntArray(0)
    private var count = 0
    private var index = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        tokenize(startOffset, endOffset)
        index = 0
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = if (index < count) types[index] else null

    override fun getTokenStart(): Int = if (index < count) starts[index] else bufferEnd

    override fun getTokenEnd(): Int = if (index < count) ends[index] else bufferEnd

    override fun advance() {
        if (index < count) index++
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd

    // ----- tokenization ----------------------------------------------------

    private fun tokenize(startOffset: Int, endOffset: Int) {
        count = 0
        val capacity = maxOf(16, (endOffset - startOffset) / 3)
        types = arrayOfNulls(capacity)
        starts = IntArray(capacity)
        ends = IntArray(capacity)

        val delims = ArrayList<IElementType>()
        // Last and second-to-last significant tokens as the compiler sees them
        // (`>>` counts as one token here, inserted semicolons count).
        var last: IElementType? = null
        var beforeLast: IElementType? = null
        // Index in the output after which an automatic semicolon goes.
        var insertAfter = -1

        fun maybeInsertSemi() {
            val l = last ?: return
            val top = delims.lastOrNull()
            if (top == SlangTypes.LPAREN || top == SlangTypes.LBRACKET) return
            val keywordName = beforeLast == SlangTypes.DOT && l in KEYWORDS
            if (!endsStatement(l) && !keywordName) return
            val at = ends[insertAfter]
            insert(insertAfter + 1, SlangTypes.SEMI, at, at)
            insertAfter += 1
            beforeLast = l
            last = SlangTypes.SEMI
        }

        flex.start(buffer, startOffset, endOffset, 0)
        while (true) {
            val type = flex.tokenType ?: break
            val s = flex.tokenStart
            val e = flex.tokenEnd
            when (type) {
                TokenType.WHITE_SPACE -> {
                    if (containsNewline(s, e)) maybeInsertSemi()
                    add(type, s, e)
                }
                SlangTokenTypes.BLOCK_COMMENT -> {
                    add(type, s, e)
                    // A comment spanning lines acts like a newline.
                    if (containsNewline(s, e)) maybeInsertSemi()
                }
                SlangTokenTypes.LINE_COMMENT -> add(type, s, e)
                // The compiler skips unknown characters without a token.
                TokenType.BAD_CHARACTER -> add(type, s, e)
                else -> {
                    when (type) {
                        SlangTypes.SHR -> {
                            add(SlangTypes.GT, s, s + 1)
                            add(SlangTypes.GT, s + 1, e)
                        }
                        SlangTypes.SHREQ -> {
                            add(SlangTypes.GT, s, s + 1)
                            add(SlangTypes.GE, s + 1, e)
                        }
                        else -> add(type, s, e)
                    }
                    when (type) {
                        SlangTypes.LPAREN, SlangTypes.LBRACKET, SlangTypes.LBRACE -> delims.add(type)
                        SlangTypes.RPAREN, SlangTypes.RBRACKET, SlangTypes.RBRACE ->
                            if (delims.isNotEmpty()) delims.removeAt(delims.size - 1)
                    }
                    beforeLast = last
                    last = type
                    insertAfter = count - 1
                }
            }
            flex.advance()
        }
        maybeInsertSemi()
    }

    private fun containsNewline(start: Int, end: Int): Boolean {
        for (i in start until end) {
            if (buffer[i] == '\n') return true
        }
        return false
    }

    private fun add(type: IElementType, start: Int, end: Int) = insert(count, type, start, end)

    private fun insert(at: Int, type: IElementType, start: Int, end: Int) {
        if (count == types.size) {
            val n = types.size * 2
            types = types.copyOf(n)
            starts = starts.copyOf(n)
            ends = ends.copyOf(n)
        }
        if (at < count) {
            System.arraycopy(types, at, types, at + 1, count - at)
            System.arraycopy(starts, at, starts, at + 1, count - at)
            System.arraycopy(ends, at, ends, at + 1, count - at)
        }
        types[at] = type
        starts[at] = start
        ends[at] = end
        count++
    }

    companion object {
        /** `TokenKind::keyword` of token.rs (including `_`). */
        @JvmField
        val KEYWORDS: Set<IElementType> = setOf(
            SlangTypes.NAMESPACE, SlangTypes.IMPORT, SlangTypes.AS, SlangTypes.STRUCT,
            SlangTypes.INTERFACE, SlangTypes.ENUM, SlangTypes.FUNC, SlangTypes.STATIC,
            SlangTypes.ASYNC, SlangTypes.AWAIT, SlangTypes.VAR, SlangTypes.CONST,
            SlangTypes.IF, SlangTypes.ELSE, SlangTypes.FOR, SlangTypes.IN,
            SlangTypes.SWITCH, SlangTypes.CASE, SlangTypes.DEFAULT, SlangTypes.RETURN,
            SlangTypes.BREAK, SlangTypes.CONTINUE, SlangTypes.DEFER, SlangTypes.TRY,
            SlangTypes.CATCH, SlangTypes.NEW, SlangTypes.NIL, SlangTypes.TRUE,
            SlangTypes.FALSE, SlangTypes.MAP, SlangTypes.THIS, SlangTypes.UNDERSCORE,
        )

        /** `TokenKind::ends_statement` of token.rs. */
        @JvmField
        val ENDS_STATEMENT: Set<IElementType> = setOf(
            SlangTypes.IDENTIFIER,
            SlangTypes.INT_LITERAL, SlangTypes.FLOAT_LITERAL,
            SlangTypes.STRING_LITERAL, SlangTypes.RAW_STRING_LITERAL, SlangTypes.CHAR_LITERAL,
            SlangTypes.RETURN, SlangTypes.BREAK, SlangTypes.CONTINUE,
            SlangTypes.NIL, SlangTypes.TRUE, SlangTypes.FALSE, SlangTypes.THIS, SlangTypes.UNDERSCORE,
            SlangTypes.RPAREN, SlangTypes.RBRACE, SlangTypes.RBRACKET,
            SlangTypes.QUESTION, SlangTypes.BANG,
            // closing generic arguments: `var nf NotFound<string>`
            SlangTypes.GT, SlangTypes.SHR,
        )

        /** Whether a newline directly after [type] ends a statement. */
        @JvmStatic
        fun endsStatement(type: IElementType): Boolean = type in ENDS_STATEMENT
    }
}
