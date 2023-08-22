package org.jetbrains.bsp.bazel.languages.starlark

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
import org.jetbrains.bsp.bazel.languages.starlark.psi.StarlarkFile

class StarlarkParserDefinition : ParserDefinition {
  private val file = IFileElementType(StarlarkLanguage)

  override fun createLexer(project: Project?): Lexer =
    TODO()

  override fun createParser(project: Project?): PsiParser =
    TODO()

  override fun getFileNodeType(): IFileElementType =
    file

  override fun getWhitespaceTokens(): TokenSet =
    TODO()

  override fun getCommentTokens(): TokenSet =
    TODO()

  override fun getStringLiteralElements(): TokenSet =
    TODO()

  override fun createElement(node: ASTNode?): PsiElement =
    TODO()

  override fun createFile(viewProvider: FileViewProvider): PsiFile =
    StarlarkFile(viewProvider)
}
