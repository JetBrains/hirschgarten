package org.jetbrains.bazel.languages.starlark.psi.functions

import com.intellij.lang.ASTNode
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import javax.swing.Icon

@ApiStatus.Internal
abstract class StarlarkParameter(node: ASTNode) : StarlarkNamedElement(node) {
  override fun getIcon(flags: Int): Icon? = PlatformIcons.PARAMETER_ICON

  override fun getUseScope(): SearchScope {
    val callable = PsiTreeUtil
      .findFirstParent(this) { it is StarlarkCallable }
      ?: return super.getUseScope()
    return LocalSearchScope(callable)
  }
}
