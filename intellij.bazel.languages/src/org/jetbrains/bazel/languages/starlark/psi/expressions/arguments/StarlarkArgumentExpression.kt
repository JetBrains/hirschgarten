package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments

import com.intellij.lang.ASTNode
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiReference
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport

@ApiStatus.Internal
class StarlarkArgumentExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkArgumentElement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitArgumentExpression(this)

  override fun getReference(): PsiReference? = serviceOrNull<StarlarkReferenceSupport>()?.getReference(this)
}
