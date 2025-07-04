package org.jetbrains.bazel.languages.starlark.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement

class StarlarkRefactoringSupportProvider : RefactoringSupportProvider() {
  override fun isSafeDeleteAvailable(element: PsiElement): Boolean =
    element is StarlarkFile
      || element is StarlarkTargetExpression
      || element is StarlarkFunctionDeclaration
      || element is StarlarkAssignmentStatement
}
