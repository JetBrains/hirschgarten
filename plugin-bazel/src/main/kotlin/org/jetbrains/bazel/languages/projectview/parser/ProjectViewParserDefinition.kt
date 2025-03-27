package org.jetbrains.bazel.languages.projectview.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
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
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementType
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementTypes
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewLexer
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile

class ProjectViewParserDefinition : ParserDefinition {
  override fun createLexer(project: Project?): Lexer = ProjectViewLexer()

  override fun createParser(project: Project?): PsiParser =
    PsiParser { root, builder ->
      val rootMarker = builder.mark()
      ProjectViewParser(builder).parseFile()
      rootMarker.done(root)
      builder.treeBuilt
    }

  override fun getFileNodeType(): IFileElementType = ProjectViewElementTypes.FILE

  override fun getWhitespaceTokens(): TokenSet = TokenSet.create(ProjectViewTokenType.WHITESPACE)

  override fun getCommentTokens(): TokenSet = TokenSet.create(ProjectViewTokenType.COMMENT)

  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

  override fun createElement(node: ASTNode): PsiElement {
    val type = node.elementType
    if (type is ProjectViewElementType) {
      return type.createElement(node)
    }
    return ASTWrapperPsiElement(node)
  }

  override fun createFile(viewProvider: FileViewProvider): PsiFile = ProjectViewPsiFile(viewProvider)
}
