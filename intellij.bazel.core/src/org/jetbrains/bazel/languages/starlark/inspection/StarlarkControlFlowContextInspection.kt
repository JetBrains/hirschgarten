package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkBreakStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkContinueStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkReturnStatement


@ApiStatus.Internal
class StarlarkControlFlowContextInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = BreakContinueVisitor(holder)

  class BreakContinueVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitBreakStatement(node: StarlarkBreakStatement) {
      if (node.parentOfType<StarlarkForStatement>() == null) {
        holder.registerProblem(node, StarlarkBundle.message("inspection.description.break.outside.for.loop"))
      }
    }

    override fun visitContinueStatement(node: StarlarkContinueStatement) {
      if (node.parentOfType<StarlarkForStatement>() == null) {
        holder.registerProblem(node, StarlarkBundle.message("inspection.description.continue.outside.for.loop"))
      }
    }

    override fun visitReturnStatement(node: StarlarkReturnStatement) {
      if (node.parentOfType<StarlarkFunctionDeclaration>() == null) {
        holder.registerProblem(node, StarlarkBundle.message("inspection.description.return.outside.function"))
      }
    }
  }
}
