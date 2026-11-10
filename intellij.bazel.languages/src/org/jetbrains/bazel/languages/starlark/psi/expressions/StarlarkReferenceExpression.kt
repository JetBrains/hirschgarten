package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport
import org.jetbrains.bazel.languages.starlark.psi.bindingSite
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.psi.isBinding
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.starlarkProjectScope
import javax.swing.Icon

/** A name, or a qualified name `a.b`. A name in a target position binds the name, and then it is also the declaration. */
@ApiStatus.Internal
class StarlarkReferenceExpression(node: ASTNode) :
  StarlarkNamedElement(node),
  StarlarkQualifiableExpression {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitReferenceExpression(this)

  override fun getReference(): PsiReference? = serviceOrNull<StarlarkReferenceSupport>()?.getReference(this)

  /** The name token. For `a.b`, it is `b`. */
  fun getIdentifier(): PsiElement? = findChildByType(StarlarkTokenTypes.IDENTIFIER)

  /** The name token of a binding. A read is no declaration, so it has no name identifier. */
  override fun getNameIdentifier(): PsiElement? = getIdentifier()?.takeIf { isBinding() }

  override fun getName(): String? = getIdentifier()?.text

  override fun getIcon(flags: Int): Icon? = if (isBinding()) PlatformIcons.VARIABLE_ICON else super.getIcon(flags)

  override fun getUseScope(): SearchScope {
    if (!isBinding()) return super.getUseScope()
    if (isTopLevel() && !isFilePrivate()) return project.starlarkProjectScope()
    val parent = PsiTreeUtil
      .findFirstParent(this) { it is StarlarkCompExpression || it is StarlarkCallable }
       ?: return super.getUseScope()
    return LocalSearchScope(parent)
  }

  fun isTopLevel(): Boolean = (bindingSite() as? StarlarkAssignmentStatement)?.isTopLevel() == true

  override fun toString(): String = if (isBinding()) super.toString() else "${javaClass.simpleName}(${node.elementType})"

  private fun isFilePrivate() = name?.startsWith("_") == true
}
