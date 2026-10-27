package org.jetbrains.bazel.languages.projectview.annotation.suppress

import com.intellij.codeInsight.daemon.ChangeLocalityDetector
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType

/**
 * A comment can suppress problems of the elements below it, so a change in a comment highlights the whole file again.
 */
@Suppress("SplitModeApiUsage")
internal class ProjectViewSuppressionChangeLocalityDetector : ChangeLocalityDetector {
  override fun getChangeHighlightingDirtyScopeFor(changedElement: PsiElement): PsiElement? = when {
    changedElement is PsiComment && changedElement.elementType == ProjectViewTokenType.COMMENT -> changedElement.containingFile
    else -> null
  }
}
