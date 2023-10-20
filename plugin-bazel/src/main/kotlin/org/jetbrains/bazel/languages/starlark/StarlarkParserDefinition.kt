package org.jetbrains.bazel.languages.starlark

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
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.lexer.StarlarkIndentingLexer
import org.jetbrains.bazel.languages.starlark.parser.StarlarkParser
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile

class StarlarkParserDefinition : ParserDefinition {
  private val file = IFileElementType(StarlarkLanguage)

  override fun createLexer(project: Project?): Lexer = StarlarkIndentingLexer()

  override fun createParser(project: Project?): PsiParser = StarlarkParser()

  override fun getFileNodeType(): IFileElementType = file

  override fun getWhitespaceTokens(): TokenSet = StarlarkTokenSets.WHITESPACE

  override fun getCommentTokens(): TokenSet = StarlarkTokenSets.COMMENT

  override fun getStringLiteralElements(): TokenSet = StarlarkTokenSets.STRINGS

  override fun createElement(node: ASTNode): PsiElement = StarlarkElementTypes.createElement(node)

  override fun createFile(viewProvider: FileViewProvider): PsiFile = StarlarkFile(viewProvider)
}
