package org.jetbrains.bazel.languages.projectview.checker

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import com.intellij.psi.util.firstLeaf
import com.intellij.psi.util.parents
import com.intellij.psi.util.prevLeaf
import com.intellij.psi.util.prevLeafs
import com.intellij.psi.util.startOffset
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType

private const val ID_PATTERN = ProjectViewProblem.Id.PATTERN

/**
 * A suppression comment, with its [ids] and the [idsRange] of its ID list in the file.
 *
 * A `# suppress: <id>, <id> <free text>` comment covers the element below it, up to a blank line.
 * It has the indentation of that element.
 * The ID list ends at the first character that is not a part of an ID, a comma or a white space.
 */
internal data class ProjectViewSuppression(
  val comment: PsiComment,
  val ids: List<ProjectViewProblem.Id>,
  val idsRange: TextRange,
) {

  companion object {

    private const val DIRECTIVE_PREFIX = "# suppress: "

    private val directivePattern = Regex("""^#\s*suppress:\s*($ID_PATTERN(?:\s*,\s*$ID_PATTERN)*)""")

    /** The suppressions that belong to [element], the nearest first. */
    fun from(element: PsiElement): Sequence<ProjectViewSuppression> = element.precedingComments().mapNotNull(::parse)

    /** The text of a suppression comment that suppresses [ids]. */
    fun commentText(ids: List<ProjectViewProblem.Id>): String = DIRECTIVE_PREFIX + commentIdsText(ids)

    /** The ID list for [ids], as it stands in a suppression comment. */
    fun commentIdsText(ids: List<ProjectViewProblem.Id>): String = ids.joinToString(", ") { it.value }

    private fun parse(comment: PsiComment): ProjectViewSuppression? {
      val idList = directivePattern
                   .find(comment.text)
                   ?.groups
                   ?.get(1)
                 ?: return null
      return ProjectViewSuppression(
        comment = comment,
        ids = idList
          .value
          .split(',')
          .map { ProjectViewProblem.Id(it.trim()) },
        idsRange = TextRange(idList.range.first, idList.range.last + 1).shiftRight(comment.startOffset),
      )
    }
  }
}

internal val ProjectViewProblem.isSuppressible: Boolean
  get() = severity < ProjectViewProblem.Severity.Error

internal fun Sequence<ProjectViewProblem>.withSuppressions(): Sequence<ProjectViewProblem> = filterNot { it.isSuppressible && it.isSuppressed() }

private fun ProjectViewProblem.isSuppressed(): Boolean {
  val element = element ?: return false
  return element
    .parents(withSelf = true)
    .takeWhile { it !is PsiFile }
    .flatMap(ProjectViewSuppression::from)
    .any { id in it.ids }
}

private fun PsiElement.precedingComments(): Sequence<PsiComment> = sequence {
  val indented = firstLeaf().isIndented()
  var newlines = 0
  for (leaf in firstLeaf().prevLeafs) {
    when {
      leaf is PsiComment -> {
        if (leaf.isIndented() != indented) return@sequence
        yield(leaf)
        newlines = 0
      }
      leaf.elementType == ProjectViewTokenType.NEWLINE -> if (++newlines > 1) return@sequence
      leaf is PsiWhiteSpace || leaf.elementType == ProjectViewTokenType.INDENT -> Unit
      else -> return@sequence
    }
  }
}

private fun PsiElement.isIndented(): Boolean {
  val previous = prevLeaf() ?: return false
  return previous.elementType != ProjectViewTokenType.NEWLINE
}
