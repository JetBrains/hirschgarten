package org.jetbrains.bazel.languages.projectview.parser

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
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementTypes
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewTokenSets
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewLexer
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewFile

class ProjectViewParserDefinition(): ParserDefinition {
  private val file = IFileElementType(ProjectViewLanguage)

  override fun createLexer(project: Project?): Lexer = ProjectViewLexer()

  override fun createParser(project: Project?): PsiParser = ProjectViewParser()

  override fun getFileNodeType(): IFileElementType = file

  override fun getCommentTokens(): TokenSet = ProjectViewTokenSets.COMMENT

  override fun getWhitespaceTokens(): TokenSet = ProjectViewTokenSets.WHITESPACE

  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

  override fun createElement(node: ASTNode): PsiElement = ProjectViewElementTypes.createElement(node)

  override fun createFile(viewProvider: FileViewProvider): PsiFile = ProjectViewFile(viewProvider)
}
