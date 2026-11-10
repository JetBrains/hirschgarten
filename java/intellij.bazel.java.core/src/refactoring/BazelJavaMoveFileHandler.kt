package org.jetbrains.bazel.refactoring

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject

// 263-only, fixed in master: https://code.jetbrains.team/p/ij/repositories/ultimate/reviews/233976
@ApiStatus.Internal
class BazelJavaMoveFileHandler : MoveFilesOrDirectoriesHandler() {
  override fun canMove(elements: Array<out PsiElement>, targetContainer: PsiElement?, reference: PsiReference?): Boolean {
    val file = getSingleJavaFile(elements) ?: return false
    return super.canMove(arrayOf(file), targetContainer, reference)
  }

  override fun adjustForMove(
    project: Project?,
    sourceElements: Array<out PsiElement>,
    targetElement: PsiElement?,
  ): Array<out PsiElement>? {
    val file = getSingleJavaFile(sourceElements) ?: return null
    return super.adjustForMove(project, arrayOf(file), targetElement)
  }

  // All other cases are already handled by JavaMoveFilesOrDirectoriesHandler (including moving several files)
  private fun getSingleJavaFile(elements: Array<out PsiElement>): PsiFile? {
    val psiClass = elements.singleOrNull() as? PsiClass ?: return null
    if (!psiClass.project.isBazelProject) return null
    return psiClass.containingFile
  }
}
