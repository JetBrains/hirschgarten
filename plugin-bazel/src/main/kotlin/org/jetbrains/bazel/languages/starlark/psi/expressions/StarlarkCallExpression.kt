package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolService
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.references.BazelGlobalFunctionReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkFunctionCallReference

@Suppress("UnstableApiUsage")
class StarlarkCallExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitCallExpression(this)

  override fun getReference(): PsiReference? =
    getNameNode()?.let {
      val range = it.textRange.relativeTo(this)
      StarlarkFunctionCallReference(this, range)
    }

  override fun getName(): String? = getNameNode()?.text

  fun getNameNode(): ASTNode? = getNamePsi()?.node

  fun getNamePsi(): StarlarkReferenceExpression? = findChildByType(StarlarkElementTypes.REFERENCE_EXPRESSION)

  fun getTargetName(): String? = getArgumentList()?.getNameArgumentValue()

  fun getArgumentList(): StarlarkArgumentList? = findChildrenByClass(StarlarkArgumentList::class.java).firstOrNull()

  override fun getOwnReferences(): Collection<PsiSymbolReference> {
    val name = name ?: return emptyList()
    val function = BazelGlobalFunctions.getFunctionByName(name) ?: return emptyList()
    return listOfNotNull(
      BazelGlobalFunctionReference(this, function),
      reference?.let { PsiSymbolService.getInstance().asSymbolReference(it) },
    )
  }
}
