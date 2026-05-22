package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue


@ApiStatus.Internal
class StarlarkLoadPrivateSymbolInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = LoadPrivateSymbolVisitor(holder)

  class LoadPrivateSymbolVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitStringLoadValue(node: StarlarkStringLoadValue) {
      val name = node.getStringExpression()?.getStringContents() ?: return
      if (name.firstOrNull() == '_') {
        holder.registerProblem(
          node,
          StarlarkBundle.message("inspection.description.load.private.symbol")
        )
      }
    }

    override fun visitNamedLoadValue(node: StarlarkNamedLoadValue) {
      val name = (node.lastChild as? StarlarkStringLiteralExpression)?.getStringContents() ?: return
      if (name.firstOrNull() == '_') {
        holder.registerProblem(
          node,
          StarlarkBundle.message("inspection.description.load.private.symbol")
        )
      }
    }
  }
}
