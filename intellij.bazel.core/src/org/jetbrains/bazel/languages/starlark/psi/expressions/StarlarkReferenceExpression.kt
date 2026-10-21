package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableElement

@ApiStatus.Internal
class StarlarkReferenceExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkLocalVariableElement,
  StarlarkQualifiableExpression {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitReferenceExpression(this)

  override fun getReference(): PsiReference? = serviceOrNull<StarlarkReferenceSupport>()?.getReference(this)

  override fun getNameIdentifier(): PsiElement? = findChildByType(StarlarkTokenTypes.IDENTIFIER)
  override fun getName(): String? = getNameIdentifier()?.text
}
