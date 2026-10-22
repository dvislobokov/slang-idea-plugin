package org.zell.idea.parser

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
import org.zell.idea.lang.ZellFile
import org.zell.idea.lexer.ZellLexer
import org.zell.idea.psi.ZellTokenSets
import org.zell.idea.psi.ZellTypes
import org.zell.idea.stubs.ZellStubElementTypes

/** Wires the semicolon-inserting [ZellLexer] and the generated [ZellParser]. */
class ZellParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = ZellLexer()

    override fun createParser(project: Project?): PsiParser = ZellParser()

    override fun getFileNodeType(): IFileElementType = ZellStubElementTypes.FILE

    override fun getWhitespaceTokens(): TokenSet = ZellTokenSets.WHITESPACES

    override fun getCommentTokens(): TokenSet = ZellTokenSets.COMMENTS

    override fun getStringLiteralElements(): TokenSet = ZellTokenSets.STRINGS

    override fun createElement(node: ASTNode): PsiElement = ZellTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = ZellFile(viewProvider)
}
