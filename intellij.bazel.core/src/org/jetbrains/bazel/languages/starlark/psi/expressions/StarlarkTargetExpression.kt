package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.starlarkProjectScope
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableElement
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableReference
import javax.swing.Icon

@ApiStatus.Internal
class StarlarkTargetExpression(node: ASTNode) :
  StarlarkNamedElement(node),
  StarlarkLocalVariableElement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitTargetExpression(this)

  override fun getReference(): PsiReference = StarlarkLocalVariableReference(this, true)

  override fun getIcon(flags: Int): Icon? = PlatformIcons.VARIABLE_ICON

  override fun getUseScope(): SearchScope {
    if (isTopLevel() && !isFilePrivate()) return project.starlarkProjectScope()
    val parent = PsiTreeUtil
      .findFirstParent(this) { it is StarlarkCompExpression || it is StarlarkCallable }
       ?: return super.getUseScope()
    return LocalSearchScope(parent)
  }

  fun isTopLevel(): Boolean {
    val parent = parent
    val nextParent = parent.parent
    val assignmentAncestor = when (parent) {
      is StarlarkAssignmentStatement -> parent
      is StarlarkTupleExpression if nextParent is StarlarkAssignmentStatement -> nextParent
      else -> return false
    }
    return assignmentAncestor.isTopLevel()
  }

  private fun isFilePrivate() = name?.startsWith("_") == true
}
