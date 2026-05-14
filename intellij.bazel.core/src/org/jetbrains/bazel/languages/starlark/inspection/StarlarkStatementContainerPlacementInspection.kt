package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkIfStatement


@ApiStatus.Internal
class StarlarkStatementContainerPlacementInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = StatementContainerVisitor(holder)

  class StatementContainerVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitForStatement(node: StarlarkForStatement) {
      if (node.parent is StarlarkFile) {
        val description = when (holder.file.name) {
          in Constants.BUILD_FILE_NAMES -> StarlarkBundle.message("inspection.description.for.placement.build")
          in WORKSPACE_ONLY_FILE_NAMES -> StarlarkBundle.message("inspection.description.for.placement.workspace")
          else -> StarlarkBundle.message("inspection.description.for.placement")
        }
        holder.registerProblem(node, description)
      }
    }

    override fun visitIfStatement(node: StarlarkIfStatement) {
      if (node.parent is StarlarkFile) {
        val description = when (holder.file.name) {
          in Constants.BUILD_FILE_NAMES -> StarlarkBundle.message("inspection.description.if.placement.build")
          in WORKSPACE_ONLY_FILE_NAMES -> StarlarkBundle.message("inspection.description.if.placement.workspace")
          else -> StarlarkBundle.message("inspection.description.if.placement")
        }
        holder.registerProblem(node, description)
      }
    }
  }

  companion object {
    private val WORKSPACE_ONLY_FILE_NAMES = arrayOf(Constants.WORKSPACE_FILE_NAME, "WORKSPACE.bazel", "WORKSPACE.bzlmod")
  }
}
