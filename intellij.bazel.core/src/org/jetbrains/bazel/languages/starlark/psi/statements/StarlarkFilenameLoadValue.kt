package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import com.intellij.util.PlatformIcons
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import javax.swing.Icon

@ApiStatus.Internal
class StarlarkFilenameLoadValue(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkLoadValue {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitFilenameLoadValue(this)

  override fun getIcon(flags: Int): Icon? = PlatformIcons.IMPORT_ICON
}
