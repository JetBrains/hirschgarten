package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.references.BazelGlobalFunctionArgumentReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkNamedArgumentReference

@Suppress("UnstableApiUsage")
class StarlarkNamedArgumentExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkArgumentElement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitNamedArgumentExpression(this)

  override fun getReference(): PsiReference? =
    getNameNode()?.let {
      val range = it.textRange.relativeTo(this)
      StarlarkNamedArgumentReference(this, range)
    }

  override fun getName(): String? = getNameNode()?.text

  fun getNameNode(): ASTNode? = node.findChildByType(StarlarkTokenTypes.IDENTIFIER)

  fun isNameArgument(): Boolean = containsArgumentWithName("name")

  fun isDepsArgument(): Boolean = containsArgumentWithName("deps")

  // Some companies may have custom macros that use another name
  fun isDependenciesArgument(): Boolean = containsArgumentWithName("dependencies")

  fun containsArgumentWithName(name: String): Boolean = node.findChildByType(StarlarkTokenTypes.IDENTIFIER)?.text == name

  fun getArgumentStringValue(): String? = findChildByClass(StarlarkStringLiteralExpression::class.java)?.getStringContents()

  override fun getOwnReferences(): Collection<BazelGlobalFunctionArgumentReference> =
    name?.let { listOf(BazelGlobalFunctionArgumentReference(this)) } ?: emptyList()
}
