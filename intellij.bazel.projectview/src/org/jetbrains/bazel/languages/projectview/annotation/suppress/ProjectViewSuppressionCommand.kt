package org.jetbrains.bazel.languages.projectview.annotation.suppress

import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModUpdateFileText
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.startOffset
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewSuppression

/**
 * The edit that adds [id] to the suppression comment of [container], or a new comment above [container] when it has none.
 */
internal fun addProjectViewSuppressionCommand(
  container: PsiElement,
  id: ProjectViewProblem.Id,
): ModCommand {
  val file = container.containingFile
  val virtualFile = file.virtualFile ?: return ModCommand.nop()
  val text = file.text
  val (range, replacement) = when (val existing = ProjectViewSuppression.from(container).firstOrNull()) {
    null -> {
      val lineStart = text.lastIndexOf('\n', container.startOffset - 1) + 1
      val range = TextRange.from(lineStart, 0)
      val replacement = "${text.substring(lineStart, container.startOffset)}${ProjectViewSuppression.commentText(listOf(id))}\n"
      range to replacement
    }
    else -> existing.idsRange to ProjectViewSuppression.commentIdsText(existing.ids + id)
  }
  return ModUpdateFileText(
    /* file = */ virtualFile,
    /* oldText = */ text,
    /* newText = */ range.replace(text, replacement),
    /* updatedRanges = */ listOf(ModUpdateFileText.Fragment(range.startOffset, range.length, replacement.length)),
  )
}
