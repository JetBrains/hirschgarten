package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableElement
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableReference
import javax.swing.Icon

class StarlarkTargetExpression(node: ASTNode) :
  StarlarkNamedElement(node),
  StarlarkLocalVariableElement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitTargetExpression(this)

  override fun getReference(): PsiReference = StarlarkLocalVariableReference(this, true)

  override fun getIcon(flags: Int): Icon? = PlatformIcons.VARIABLE_ICON
}
