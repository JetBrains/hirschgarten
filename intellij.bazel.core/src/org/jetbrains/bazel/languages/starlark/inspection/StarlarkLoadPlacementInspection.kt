package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement


@ApiStatus.Internal
class StarlarkLoadPlacementInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = LoadPlacementVisitor(holder)

  private class LoadPlacementVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitLoadStatement(node: StarlarkLoadStatement) {
      if (holder.file.name == Constants.MODULE_BAZEL_FILE_NAME) {
        holder.registerProblem(
          node,
          StarlarkBundle.message("inspection.description.load.not.allowed.in.module.bazel")
        )
      } else if (node.parent !is StarlarkFile) {
        holder.registerProblem(
          node,
          StarlarkBundle.message("inspection.description.load.not.at.top.level")
        )
      } else if (holder.file.name !in (CONFIG_FILE_NAMES) && hasPreviousNonLoadTopLevelStatement(node)) {
        holder.registerProblem(
          node,
          StarlarkBundle.message("inspection.description.load.after.statement")
        )
      }
    }

    private fun hasPreviousNonLoadTopLevelStatement(loadStatement: StarlarkLoadStatement): Boolean {
      var sibling = PsiTreeUtil.skipWhitespacesAndCommentsBackward(loadStatement)
      while (sibling != null) {
        if (sibling !is StarlarkLoadStatement) {
          return true
        }
        sibling = PsiTreeUtil.skipWhitespacesAndCommentsBackward(sibling)
      }
      return false
    }
  }

  companion object {
    private val CONFIG_FILE_NAMES = Constants.WORKSPACE_FILE_NAMES + Constants.BUILD_FILE_NAMES
  }
}
