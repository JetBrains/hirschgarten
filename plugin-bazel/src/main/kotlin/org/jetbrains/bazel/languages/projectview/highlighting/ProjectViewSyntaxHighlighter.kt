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
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenTypes

object ProjectViewSyntaxHighlighter : SyntaxHighlighterBase() {
  private val keys =
    mapOf(
      ProjectViewTokenTypes.IMPORT_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.TARGETS_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.BAZEL_BINARY_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.DIRECTORIES_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.DERIVE_TARGETS_FROM_DIRECTORIES_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.IMPORT_DEPTH_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.WORKSPACE_TYPE_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.ADDITIONAL_LANGUAGES_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.JAVA_LANGUAGE_LEVEL_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.TEST_SOURCES_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.SHARD_SYNC_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.TARGET_SHARD_SIZE_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.EXCLUDE_LIBRARY_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.BUILD_FLAGS_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.SYNC_FLAGS_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.TEST_FLAGS_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.IMPORT_RUN_CONFIGURATION_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.ANDROID_SDK_PLATFORM_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.ANDROID_MIN_SDK_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.GENERATED_ANDROID_RESOURCE_DIRECTORIES_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.TS_CONFIG_RULES_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.BUILD_MANUAL_TARGETS_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.ENABLED_RULES_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.PRODUCE_TRACE_LOG_KEYWORD to ProjectViewHighlightingColors.KEYWORD,
      ProjectViewTokenTypes.INT to ProjectViewHighlightingColors.NUMBER,
      ProjectViewTokenTypes.BOOL to ProjectViewHighlightingColors.CONST,
      ProjectViewTokenTypes.IDENTIFIER to ProjectViewHighlightingColors.IDENTIFIER,
      ProjectViewTokenTypes.STRING to ProjectViewHighlightingColors.STRING,
      ProjectViewTokenTypes.COMMENT to ProjectViewHighlightingColors.LINE_COMMENT,
    )

  override fun getHighlightingLexer(): Lexer = ProjectViewLexer()

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(keys[tokenType])
}

class ProjectViewSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter = ProjectViewSyntaxHighlighter
}
