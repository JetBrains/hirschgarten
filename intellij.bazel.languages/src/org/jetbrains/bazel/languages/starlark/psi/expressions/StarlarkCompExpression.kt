package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.boundNames
import org.jetbrains.bazel.languages.starlark.psi.isLoopTarget

@ApiStatus.Internal
abstract class StarlarkCompExpression(node: ASTNode) : StarlarkBaseElement(node) {
  fun getCompVariables(): List<StarlarkReferenceExpression> =
    children.filter { it.isLoopTarget() }.flatMap { it.boundNames() }
}
