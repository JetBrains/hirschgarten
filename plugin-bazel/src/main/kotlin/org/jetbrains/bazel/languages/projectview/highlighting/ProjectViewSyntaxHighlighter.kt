package org.jetbrains.bazel.languages.projectview.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewLexer
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType

object ProjectViewSyntaxHighlighter : SyntaxHighlighterBase() {
  private val keys =
    mapOf(
      ProjectViewTokenType.COLON to ProjectViewHighlightingColors.COLON,
      ProjectViewTokenType.COMMENT to ProjectViewHighlightingColors.COMMENT,
      ProjectViewTokenType.IDENTIFIER to ProjectViewHighlightingColors.IDENTIFIER,
      ProjectViewTokenType.SECTION_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenType.IMPORT_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenType.TRY_IMPORT_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
    )

  override fun getHighlightingLexer(): Lexer = ProjectViewLexer()

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(keys[tokenType])
}

class ProjectViewSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter = ProjectViewSyntaxHighlighter
}
