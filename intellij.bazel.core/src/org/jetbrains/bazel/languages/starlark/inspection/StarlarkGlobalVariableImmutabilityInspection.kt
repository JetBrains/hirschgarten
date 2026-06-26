package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement

@ApiStatus.Internal
class StarlarkGlobalVariableImmutabilityInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = GlobalVarVisitor(holder)

  class GlobalVarVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitFile(psiFile: PsiFile) {
      val file = psiFile as? StarlarkFile ?: return
      if (file.name in EXCLUDED_FILE_NAMES) return
      val assignmentsByName = mutableMapOf<String, MutableList<StarlarkAssignmentStatement>>()

      file.childrenOfType<StarlarkAssignmentStatement>().forEach { assignment ->
        val name = assignment.name ?: return@forEach
        assignmentsByName.getOrPut(name) { mutableListOf() }.add(assignment)
      }

      assignmentsByName.values
        .filter { it.size > 1 }
        .forEach { reassignments ->
          reassignments.drop(1).forEach { stmt ->
            holder.registerProblem(
              stmt,
              StarlarkBundle.message("inspection.description.global.variable.immutable"),
            )
          }
        }
    }
  }

  companion object {
    private val EXCLUDED_FILE_NAMES = Constants.BUILD_FILE_NAMES +
                                      arrayOf(Constants.WORKSPACE_FILE_NAME, "WORKSPACE.bazel", "WORKSPACE.bzlmod")
  }
}
