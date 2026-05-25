package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolService
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.starlarkProjectScope
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.references.BazelGlobalFunctionReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkFunctionCallReference
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Detects whether this [PsiElement] is a [StarlarkCallExpression] that is a target rule declaration in a BUILD file.
 * @see [StarlarkCallExpression.isRuleTarget]
 */
@OptIn(ExperimentalContracts::class)
@ApiStatus.Internal
fun PsiElement.isRuleTarget(): Boolean {
  contract { returns(true) implies (this@isRuleTarget is StarlarkCallExpression) }
  return this is StarlarkCallExpression && isRuleTarget()
}

/**
 * It implements [PsiNameIdentifierOwner] for target rule declarations in BUILD files.
 * A call expression is a target rule declaration when it has a `name` argument whose value is a simple string literal
 * and the containing file is a BUILD file.
 * It's communicated to the IntelliJ platform by [org.jetbrains.bazel.languages.starlark.findusages.BazelRuleTargetCallElementEvaluator].
 */
@Suppress("UnstableApiUsage")
@ApiStatus.Internal
class StarlarkCallExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  PsiNameIdentifierOwner {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitCallExpression(this)

  override fun getUseScope(): SearchScope = if (isRuleTarget()) project.starlarkProjectScope() else super.getUseScope()

  override fun getReference(): PsiReference? =
    getNameNode()?.let {
      val range = it.textRange.relativeTo(this)
      StarlarkFunctionCallReference(this, range)
    }

  override fun getName(): String? = if (isRuleTarget()) getNameAttributeValue() else null

  override fun setName(name: String): PsiElement {
    if (!isRuleTarget()) return this
    val literal = nameArgumentStringLiteral() ?: return this
    val range = ElementManipulators.getValueTextRange(literal)
    return ElementManipulators
      .getManipulator(literal)
      .handleContentChange(literal, range, name)
      ?: this
  }

  override fun getNameIdentifier(): PsiElement? = when {
      isRuleTarget() -> nameArgumentStringLiteral()
      else -> null
  }

  override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

  fun isRuleTarget(): Boolean {
    if (getNameAttributeValue() == null) return false
    val file = containingFile as? StarlarkFile ?: return false
    return file.isBuildFile()
  }

  fun getNameNode(): ASTNode? = getNamePsi()?.node

  internal fun getNamePsi(): StarlarkReferenceExpression? = findChildByType(StarlarkElementTypes.REFERENCE_EXPRESSION)

  fun getNameAttributeValue(): String? = nameArgumentStringLiteral()?.getStringContents()

  fun getCalledFunctionName(): String? = getNameNode()?.text

  fun getArgumentList(): StarlarkArgumentList? = findChildrenByClass(StarlarkArgumentList::class.java).firstOrNull()

  override fun getOwnReferences(): Collection<PsiSymbolReference> {
    val name = getCalledFunctionName() ?: return emptyList()
    val function = BazelGlobalFunctions.getFunctionByName(name) ?: return emptyList()
    return listOfNotNull(
      BazelGlobalFunctionReference(this, function),
      reference?.let { PsiSymbolService.getInstance().asSymbolReference(it) },
    )
  }

  private fun nameArgumentStringLiteral(): StarlarkStringLiteralExpression? = getArgumentList()
    ?.getNameArgument()
    ?.getValue() as? StarlarkStringLiteralExpression
}
