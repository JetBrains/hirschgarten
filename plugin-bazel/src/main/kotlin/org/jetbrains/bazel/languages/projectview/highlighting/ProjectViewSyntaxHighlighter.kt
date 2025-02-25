package org.jetbrains.bazel.languages.projectview.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewTokenTypes
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewLexer

object ProjectViewSyntaxHighlighter : SyntaxHighlighterBase() {
  private val keys =
    mapOf(
      ProjectViewTokenTypes.COLON to ProjectViewHighlightingColors.COLON,
      ProjectViewTokenTypes.COMMENT to ProjectViewHighlightingColors.LINE_COMMENT,
      ProjectViewTokenTypes.COLON to ProjectViewHighlightingColors.COLON,
      ProjectViewTokenTypes.IDENTIFIER to ProjectViewHighlightingColors.IDENTIFIER,
      ProjectViewTokenTypes.LIST_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.SCALAR_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
    )

  override fun getHighlightingLexer(): Lexer = ProjectViewLexer()

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(keys[tokenType])
}

class ProjectViewSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter = ProjectViewSyntaxHighlighter
}
