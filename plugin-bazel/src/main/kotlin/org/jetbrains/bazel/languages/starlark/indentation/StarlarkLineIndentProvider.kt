package org.jetbrains.bazel.languages.starlark.indentation

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.IndentInfo
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider
import com.intellij.psi.tree.IElementType
import com.intellij.util.text.CharArrayUtil
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

private val WHITE_SPACE_OR_COMMENT: List<IElementType> =
  StarlarkTokenSets.WHITESPACE.types.toList() +
    StarlarkTokenSets.COMMENT.types.toList()

private val BLOCK_OPENING: List<IElementType> =
  StarlarkTokenSets.OPEN_BRACKETS.types.toList() +
    listOf(StarlarkTokenTypes.COLON)

class StarlarkLineIndentProvider : LineIndentProvider {
  override fun getLineIndent(
    project: Project,
    editor: Editor,
    language: Language?,
    offset: Int,
  ): String? = if (offset > 0) getIndent(editor, offset) else ""

  private fun getIndent(editor: Editor, offset: Int): String? {
    val currentPosition = StarlarkSemanticEditorPosition(editor as EditorEx, offset)
    val previousPosition = currentPosition.getPreviousPositionExcluding(WHITE_SPACE_OR_COMMENT)
    return when {
      previousPosition.isAtAnyOf(BLOCK_OPENING) ->
        getIndentString(editor, previousPosition.startOffset())
      else -> null
    }
  }

  private fun getIndentString(editor: Editor, offset: Int): String {
    val settings: CodeStyleSettings = CodeStyle.getSettings(editor)
    val indentOptions = settings.getIndentOptions(StarlarkFileType)
    val docChars = editor.document.charsSequence

    var baseIndent = ""
    val indentStart = CharArrayUtil.shiftBackwardUntil(docChars, offset, "\n") + 1
    if (indentStart >= 0) {
      val indentEnd = CharArrayUtil.shiftForward(docChars, indentStart, " \t")
      if (indentEnd > indentStart) {
        baseIndent = docChars.subSequence(indentStart, indentEnd).toString()
      }
    }
    baseIndent += IndentInfo(0, indentOptions.INDENT_SIZE, 0).generateNewWhiteSpace(indentOptions)
    return baseIndent
  }

  override fun isSuitableFor(language: Language?): Boolean = language is StarlarkLanguage
}
