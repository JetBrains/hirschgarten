package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.annotations.ApiStatus
import javax.swing.Icon

@ApiStatus.Internal
fun getCompletionLookupElement(
  name: String,
  icon: Icon,
  priority: Double = 0.0,
  presentableText: String? = null,
  tailText: String = "",
  /**
   * isExpressionFinished indicates whether the lookup element finishes the expression:
   * `true` if the lookup element completes the entire expression (or its final part),
   * `false` if the lookup element completes only a part of the expression (for example,
   * when completing the package path to a class with subsequent subpackages)
   */
  isExpressionFinished: Boolean = true,
): LookupElement =
  PrioritizedLookupElement.withPriority(
    LookupElementBuilder
      .create("\"" + name + "\"")
      .withIcon(icon)
      .withPresentableText(presentableText ?: name)
      .withTailText(tailText, true)
      .withInsertHandler { context, _ ->
        // This prevents inserting a duplicate quote at the end.
        val document = context.document
        val offset = context.tailOffset
        if (offset < document.textLength && document.charsSequence[offset] == '"') {
          document.deleteString(offset, offset + 1)
        }

        // If the expression is not finished, continue completion inside the quotation mark.
        if (!isExpressionFinished) {
          context.editor.caretModel.moveToOffset(offset - 1)
          AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
        }
      },
    priority,
  )
