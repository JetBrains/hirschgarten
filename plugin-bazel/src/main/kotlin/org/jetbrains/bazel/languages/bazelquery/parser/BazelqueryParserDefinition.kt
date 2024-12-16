package org.jetbrains.bazel.languages.bazelquery.parser

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
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.lexer.BazelqueryLexer
import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryFile

class BazelqueryParserDefinition : ParserDefinition {
    private val file = IFileElementType(BazelqueryLanguage)

    override fun createLexer(project: Project?): Lexer = BazelqueryLexer()

    override fun createParser(project: Project?): PsiParser =
        PsiParser { root, builder ->
            Parsing(root, builder).parseFile()
        }

    override fun getFileNodeType(): IFileElementType = file

    override fun getWhitespaceTokens(): TokenSet = BazelqueryTokenSets.WHITE_SPACES

    override fun getCommentTokens(): TokenSet = BazelqueryTokenSets.COMMENTS

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement = BazelqueryElementTypes.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = BazelqueryFile(viewProvider)
}
