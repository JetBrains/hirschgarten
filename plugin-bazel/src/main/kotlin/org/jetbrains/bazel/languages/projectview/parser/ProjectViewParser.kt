package org.jetbrains.bazel.languages.projectview.parser

import com.intellij.lang.PsiBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementType
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementTypes
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType

class ProjectViewParser(private val builder: PsiBuilder) {
  fun parseFile() {
    builder.setDebugMode(ApplicationManager.getApplication().isUnitTestMode)
    while (!builder.eof()) {
      if (matches(ProjectViewTokenType.NEWLINE)) {
        continue
      }
      parseBlock()
    }
  }

  /** Parse an import statement or a section. */
  fun parseBlock() {
    val marker = builder.mark()
    when (getCurrentTokenType()) {
      ProjectViewTokenType.SECTION_KEYWORD -> {
        ProjectViewSection.KEYWORD_MAP[builder.tokenText]?.let { parser ->
          builder.advanceLexer()
          expect(ProjectViewTokenType.COLON)
          when (parser) {
            is ProjectViewSection.Parser.Scalar -> {
              parseItem(ProjectViewElementTypes.SECTION_ITEM)
            }
            is ProjectViewSection.Parser.List<*> -> {
              skipToNextLine()
              parseListItems()
            }
          }
          marker.done(ProjectViewElementTypes.SECTION)
          return
        }
      }
      ProjectViewTokenType.IMPORT_KEYWORD -> {
        builder.advanceLexer()
        parseItem(ProjectViewElementTypes.IMPORT_ITEM)
        builder.advanceLexer()
        marker.done(ProjectViewElementTypes.IMPORT)
        return
      }
    }
    // Error handling
    when {
      matches(ProjectViewTokenType.INDENT) ->
        skipBlockAndError(marker, "Indented lines must be items of a list section.")
      matches(ProjectViewTokenType.COLON) ->
        skipBlockAndError(marker, "A line cannot begin with a colon.")
      else -> skipBlockAndError(marker, "Unrecognized keyword: ${builder.tokenText}")
    }
  }

  fun parseListItems() {
    while (!builder.eof()) {
      if (matches(ProjectViewTokenType.NEWLINE)) {
        continue
      }
      if (!matches(ProjectViewTokenType.INDENT)) {
        return
      }
      parseItem(ProjectViewElementTypes.SECTION_ITEM)
    }
  }

  fun parseItem(itemType: ProjectViewElementType) {
    val item = builder.mark()
    skipToEndOfLine()
    item.done(itemType)
  }

  fun skipBlockAndError(marker: PsiBuilder.Marker, message: String) {
    while (!builder.eof()) {
      if (builder.lookAhead(0) == ProjectViewTokenType.NEWLINE &&
        builder.lookAhead(1) in blockHeaders
      ) {
        builder.advanceLexer()
        break
      }
      builder.advanceLexer()
    }
    marker.error(message)
  }

  fun skipToEndOfLine() {
    while (!builder.eof()) {
      if (getCurrentTokenType() == ProjectViewTokenType.NEWLINE) {
        return
      }
      builder.advanceLexer()
    }
  }

  fun skipToNextLine() {
    while (!builder.eof()) {
      if (matches(ProjectViewTokenType.NEWLINE)) {
        return
      }
      builder.advanceLexer()
    }
  }

  /**
   * Consume the current token if matches the provided token type and return true.
   * Otherwise, return false and mark an error.
   */
  fun expect(type: ProjectViewTokenType): Boolean {
    if (matches(type)) {
      return true
    }
    builder.error(BazelPluginBundle.message("bazel.language.project.parser.error", type))
    return false
  }

  /**
   * Consume the current token if matches the provided token type and return true.
   * Otherwise, return false.
   */
  fun matches(type: ProjectViewTokenType): Boolean {
    if (getCurrentTokenType() == type) {
      builder.advanceLexer()
      return true
    }
    return false
  }

  fun getCurrentTokenType(): IElementType? = builder.tokenType

  companion object {
    private val blockHeaders =
      setOf(
        ProjectViewTokenType.IDENTIFIER,
        ProjectViewTokenType.IMPORT_KEYWORD,
        ProjectViewTokenType.SECTION_KEYWORD,
      )
  }
}
