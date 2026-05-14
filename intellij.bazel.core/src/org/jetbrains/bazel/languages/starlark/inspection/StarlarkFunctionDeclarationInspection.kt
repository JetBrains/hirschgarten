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
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration


@ApiStatus.Internal
class StarlarkFunctionDeclarationInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    if (holder.file.name in AVAILABLE_FILE_NAMES) FunctionVisitor(holder)
    else PsiElementVisitor.EMPTY_VISITOR

  class FunctionVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitFunctionDeclaration(node: StarlarkFunctionDeclaration) {
      val description = when (holder.file.name) {
        Constants.MODULE_BAZEL_FILE_NAME -> StarlarkBundle.message("inspection.description.function.declaration.in.module.file")
        in Constants.BUILD_FILE_NAMES -> StarlarkBundle.message("inspection.description.function.declaration.in.build.file")
        else -> StarlarkBundle.message("inspection.description.function.declaration.in.workspace.file")
      }
      holder.registerProblem(node, description)
    }
  }

  companion object {
    private val AVAILABLE_FILE_NAMES = Constants.BUILD_FILE_NAMES + Constants.WORKSPACE_FILE_NAMES
  }
}
