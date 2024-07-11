package org.jetbrains.bazel.languages.starlark.psi.functions

import com.intellij.lang.ASTNode
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import javax.swing.Icon

class StarlarkFunctionDeclaration(node: ASTNode) : StarlarkNamedElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitFunctionDeclaration(this)

  override fun getIcon(flags: Int): Icon? = PlatformIcons.FUNCTION_ICON

  fun getParameters(): List<StarlarkNamedParameter> =
    findChildByClass(StarlarkParameterList::class.java)?.getParameters().orEmpty()
}
