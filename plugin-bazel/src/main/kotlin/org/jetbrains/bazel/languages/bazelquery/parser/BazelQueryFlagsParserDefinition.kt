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
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.lexer.BazelQueryLexer
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFlagsFile

class BazelQueryFlagsParserDefinition : ParserDefinition {
  private val file = IFileElementType(BazelQueryFlagsLanguage)

  override fun createLexer(project: Project?): Lexer = BazelQueryLexer()

  override fun createParser(project: Project?): PsiParser =
    PsiParser { root, builder ->
      ParsingFlags(root, builder).parseFile()
    }

  override fun getFileNodeType(): IFileElementType = file

  override fun getWhitespaceTokens(): TokenSet = BazelQueryTokenSets.WHITE_SPACES

  override fun getCommentTokens(): TokenSet = BazelQueryTokenSets.COMMENTS

  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

  override fun createElement(node: ASTNode): PsiElement = BazelQueryElementTypes.createElement(node)

  override fun createFile(viewProvider: FileViewProvider): PsiFile = BazelQueryFlagsFile(viewProvider)
}
