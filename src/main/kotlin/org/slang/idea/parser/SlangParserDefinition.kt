package org.slang.idea.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.slang.idea.lang.SlangFile
import org.slang.idea.lexer.SlangLexer
import org.slang.idea.psi.SlangTokenSets
import org.slang.idea.psi.SlangTypes
import org.slang.idea.stubs.SlangStubElementTypes

/** Wires the semicolon-inserting [SlangLexer] and the generated [SlangParser]. */
class SlangParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = SlangLexer()

    override fun createParser(project: Project?): PsiParser = SlangParser()

    override fun getFileNodeType(): IFileElementType = SlangStubElementTypes.FILE

    override fun getWhitespaceTokens(): TokenSet = SlangTokenSets.WHITESPACES

    override fun getCommentTokens(): TokenSet = SlangTokenSets.COMMENTS

    override fun getStringLiteralElements(): TokenSet = SlangTokenSets.STRINGS

    override fun createElement(node: ASTNode): PsiElement = SlangTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = SlangFile(viewProvider)
}
