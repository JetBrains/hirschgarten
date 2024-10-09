package org.jetbrains.bazel.languages.starlark.indentation

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.psi.tree.IElementType

class StarlarkSemanticEditorPosition(private var editor: EditorEx, offset: Int) {
  private var iterator: HighlighterIterator = editor.highlighter.createIterator(offset)
  private var chars: CharSequence = this.editor.document.charsSequence

  fun getPreviousPositionExcluding(elements: List<IElementType>): StarlarkSemanticEditorPosition {
    val position: StarlarkSemanticEditorPosition = copy()
    position.moveBeforeAnyOf(elements)
    return position
  }

  private fun copy(): StarlarkSemanticEditorPosition = StarlarkSemanticEditorPosition(editor, startOffset())

  fun startOffset(): Int = iterator.start

  private fun moveBeforeAnyOf(elements: List<IElementType>) {
    if (!iterator.atEnd()) iterator.retreat()
    while (isAtAnyOf(elements)) {
      iterator.retreat()
    }
  }

  fun isAt(element: IElementType): Boolean = !iterator.atEnd() && element == getType()

  fun isAtAnyOf(elements: List<IElementType>): Boolean = !iterator.atEnd() && elements.any { it == getType() }

  fun getType(): IElementType = iterator.tokenType

  override fun toString(): String =
    if (iterator.atEnd()) {
      "atEnd"
    } else {
      iterator.tokenType.toString() + "=>" +
        chars.subSequence(iterator.start, Integer.min(iterator.start + 255, chars.length))
    }
}
