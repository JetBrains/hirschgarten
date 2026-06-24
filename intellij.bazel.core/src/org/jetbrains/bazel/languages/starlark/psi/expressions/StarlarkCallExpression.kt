package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolService
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.elementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.starlarkProjectScope
import javax.swing.Icon
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.references.BazelGlobalFunctionReference
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
 * Whether this call expression is a top-level function call in a BUILD or MODULE.bazel file.
 */
@OptIn(ExperimentalContracts::class)
internal fun PsiElement.isBazelFileTopLevelCall(): Boolean {
  contract { returns(true) implies (this@isBazelFileTopLevelCall is StarlarkCallExpression) }
  if (this !is StarlarkCallExpression) return false
  // when StarlarkCallExpression is a top-level call, it is a child of StarlarkExpressionStatement, which is a child of the StarlarkFile
  val file = parent?.parent as? StarlarkFile ?: return false
  val fileType = file.getBazelFileType()
  return fileType == BazelFileType.BUILD || fileType == BazelFileType.MODULE
}

/**
 * It implements [PsiNameIdentifierOwner] for target rule declarations in BUILD files.
 * A call expression is a target rule declaration when it has a `name` argument whose value is a simple string literal
 * and the containing file is a BUILD file.
 * It's communicated to the IntelliJ platform by [org.jetbrains.bazel.languages.starlark.findusages.BazelRuleTargetCallElementEvaluator].
 */
@ApiStatus.Internal
class StarlarkCallExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  PsiNameIdentifierOwner {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitCallExpression(this)

  override fun getUseScope(): SearchScope = if (isRuleTarget()) project.starlarkProjectScope() else super.getUseScope()

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

  override fun getPresentation(): ItemPresentation? = when {
      isRuleTarget() ->  BazelTargetPresentation(this)
      else -> null
  }

  fun isRuleTarget(): Boolean {
    if (getNameAttributeValue() == null) return false
    val file = containingFile as? StarlarkFile ?: return false
    return file.isBuildFile()
  }

  fun getNameAttributeValue(): String? = nameArgumentStringLiteral()?.getStringContents()
  fun getCalledFunctionName(): String? = (getCalledExpression() as? StarlarkReferenceExpression)?.text
  fun getCalledExpression(): PsiElement? = findChildByType(StarlarkElementTypes.EXPRESSIONS)
  fun getArgumentList(): StarlarkArgumentList? = findChildrenByClass(StarlarkArgumentList::class.java).firstOrNull()

  override fun getOwnReferences(): Collection<PsiSymbolReference> {
    val name = getCalledFunctionName() ?: return emptyList()
    val function = BazelGlobalFunctions.getFunctionByName(name, project) ?: return emptyList()
    return listOfNotNull(
      BazelGlobalFunctionReference(this, function),
      getCalledExpression()?.reference?.let { PsiSymbolService.getInstance().asSymbolReference(it) },
    )
  }

  private fun nameArgumentStringLiteral(): StarlarkStringLiteralExpression? = getArgumentList()
    ?.getNameArgument()
    ?.getValue() as? StarlarkStringLiteralExpression
}

private class BazelTargetPresentation(private val target: StarlarkCallExpression) : ItemPresentation {
  override fun getPresentableText(): String? = target.getNameAttributeValue()

  override fun getIcon(unused: Boolean): Icon = BazelPluginIcons.bazel

  override fun getLocationString(): String? {
    val virtualFile = target.containingFile?.virtualFile ?: return null
    val basePath = target.project.basePath ?: return virtualFile.presentableUrl
    return virtualFile.path.removePrefix("$basePath/")
  }
}
