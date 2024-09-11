package org.jetbrains.bazel.languages.starlark.psi.functions

import com.intellij.lang.ASTNode
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import javax.swing.Icon

abstract class StarlarkParameter(node: ASTNode) : StarlarkNamedElement(node) {
  override fun getIcon(flags: Int): Icon? = PlatformIcons.PARAMETER_ICON
}
