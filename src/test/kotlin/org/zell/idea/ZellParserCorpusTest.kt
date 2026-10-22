package org.zell.idea

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import org.zell.idea.parser.ZellParserDefinition
import java.io.File

/**
 * The mandatory corpus test (CLAUDE.md): the PSI parser must accept every
 * file the compiler accepts. Parses all `.zl` files of examples/, tests/spec/
 * and std/ of the zell checkout in build/zell (tools/sync-zell.sh) and
 * fails on any PsiErrorElement.
 *
 * Skipped: files whose first line is a `// error:` directive naming a
 * *lexer* diagnostic of compiler/src/lexer.rs (those programs are not
 * lexically valid; every other compile-fail program is syntactically valid
 * and must parse).
 */
class ZellParserCorpusTest : ParsingTestCase("", "zl", ZellParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"

    fun testCorpus() {
        val root = File(System.getProperty("zell.corpus") ?: "build/zell")
        assertTrue("zell corpus not found at ${root.absolutePath}: run tools/sync-zell.sh", File(root, "examples").isDirectory)

        val files = listOf("examples", "tests/spec", "std")
            .map { File(root, it) }
            .filter { it.isDirectory }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "zl" }.toList() }
            .sortedBy { it.path }
        assertTrue("no .zl files under ${root.absolutePath}", files.size > 100)

        val failures = ArrayList<String>()
        var parsed = 0
        for (f in files) {
            val text = f.readText().replace("\r\n", "\n")
            if (isLexicallyInvalid(text)) continue
            val psi = createFile(f.name, text)
            assertEquals("PSI text differs from the file: ${f.path}", text, psi.text)
            parsed++
            val errors = PsiTreeUtil.collectElementsOfType(psi, PsiErrorElement::class.java)
            if (errors.isNotEmpty()) {
                val e = errors.first()
                val offset = e.textRange.startOffset
                val line = text.substring(0, offset).count { it == '\n' } + 1
                val lineText = text.lineSequence().drop(line - 1).firstOrNull()?.trim().orEmpty()
                failures.add("${f.relativeTo(root)}:$line: ${e.errorDescription} | $lineText")
            }
        }
        if (failures.isNotEmpty()) {
            fail(
                "${failures.size} of $parsed corpus files have parse errors:\n" +
                    failures.take(50).joinToString("\n") +
                    (if (failures.size > 50) "\n..." else ""),
            )
        }
    }

    companion object {
        /** Diagnostics of compiler/src/lexer.rs (substrings of the messages). */
        private val LEXER_ERRORS = listOf(
            "unterminated", "unknown escape sequence", "incomplete escape sequence", "empty char literal",
            "char literal must contain", "invalid digit", "literal has no digits", "float exponent has no digits",
            "invalid suffix", "unexpected character", "unicode escape", "invalid unicode scalar value", "expected `{` after",
        )

        fun isLexicallyInvalid(text: String): Boolean {
            val first = text.lineSequence().firstOrNull()?.trim() ?: return false
            if (!first.startsWith("// error:")) return false
            val message = first.removePrefix("// error:").trim()
            return LEXER_ERRORS.any { message.startsWith(it) }
        }
    }
}
