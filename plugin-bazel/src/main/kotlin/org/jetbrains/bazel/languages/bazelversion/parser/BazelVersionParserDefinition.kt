package org.jetbrains.bazel.languages.bazelversion.parser

import com.intellij.lang.ASTFactory
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.EmptyLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PlainTextTokenTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.bazel.languages.bazelversion.BazelVersionLanguage
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionFile

/**
 * @see com.intellij.openapi.fileTypes.PlainTextParserDefinition
 */
class BazelVersionParserDefinition : ParserDefinition {
  // avoid creating new IFileElementType on each getFileNodeType
  // to ensure IFileElementType is registered only once inside an internal registry
  val file = object : IFileElementType(BazelVersionLanguage) {
    override fun parseContents(chameleon: ASTNode): ASTNode = ASTFactory.leaf(PlainTextTokenTypes.PLAIN_TEXT, chameleon.chars)
  }

  override fun createLexer(project: Project?): Lexer = EmptyLexer()

  override fun createParser(project: Project?): PsiParser = throw UnsupportedOperationException()

  override fun getFileNodeType(): IFileElementType = file

  override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

  override fun createElement(node: ASTNode?): PsiElement = PsiUtilCore.NULL_PSI_ELEMENT

  override fun createFile(viewProvider: FileViewProvider): PsiFile = BazelVersionFile(viewProvider)
}
