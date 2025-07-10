package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import javax.swing.Icon

class StarlarkFilenameLoadValue(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkLoadValue {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitFilenameLoadValue(this)

  override fun getIcon(flags: Int): Icon? = PlatformIcons.IMPORT_ICON

  fun getStringLiteralExpression(): StarlarkStringLiteralExpression? = findChildByType(StarlarkElementTypes.STRING_LITERAL_EXPRESSION)
}
