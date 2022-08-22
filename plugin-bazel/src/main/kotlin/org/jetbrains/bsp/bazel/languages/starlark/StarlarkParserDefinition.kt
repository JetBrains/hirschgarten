package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bsp.bazel.languages.starlark.parser.StarlarkParser
import org.jetbrains.bsp.bazel.languages.starlark.psi.StarlarkFile

class StarlarkParserDefinition : ParserDefinition {

    private val file = IFileElementType(StarlarkLanguage)


    override fun createLexer(project: Project?): Lexer =
        StarlarkLexerAdapter

    override fun createParser(project: Project?): PsiParser =
        StarlarkParser()

    override fun getFileNodeType(): IFileElementType =
        file

    override fun getWhitespaceTokens(): TokenSet =
        TokenSet.create(TokenType.WHITE_SPACE)

    override fun getCommentTokens(): TokenSet =
        TokenSet.create(StarlarkTypes.COMMENT)

    override fun getStringLiteralElements(): TokenSet =
        TokenSet.create(StarlarkTypes.STRING)

    override fun createElement(node: ASTNode?): PsiElement =
        StarlarkTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile =
        StarlarkFile(viewProvider)
}
