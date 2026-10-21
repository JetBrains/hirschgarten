package org.jetbrains.bazel.languages.starlark.references

import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolService
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement.Companion.relativeTo
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue

@Suppress("UnstableApiUsage")
internal class StarlarkReferenceSupportImpl : StarlarkReferenceSupport {
  override fun getReference(element: StarlarkReferenceExpression): PsiReference? = with(element) {
    when {
      isThrowaway() -> null
      isQualified() -> StarlarkQualifiedReferenceExpressionReference(this)
      (parent as? StarlarkCallExpression)?.getCalledExpression() == this && !isBeforeDot() -> {
        when (val identifier = getNameIdentifier()) {
          null -> null
          else -> StarlarkFunctionCallReference(this,  identifier.textRangeInParent)
        }
      }
      else -> StarlarkLocalVariableReference(this, false)
    }
  }

  override fun getReference(element: StarlarkTargetExpression): PsiReference = StarlarkLocalVariableReference(element, true)

  override fun getReference(element: StarlarkArgumentExpression): PsiReference = StarlarkArgumentReference(element, element.textRange)

  override fun getReference(element: StarlarkNamedArgumentExpression): PsiReference? = with(element) {
    getNameNode()?.let {
      val range = it.textRange.relativeTo(this)
      StarlarkNamedArgumentReference(this, range)
    }
  }

  override fun getOwnReferences(element: StarlarkNamedArgumentExpression): Collection<PsiSymbolReference> = with(element) {
    name?.let { listOf(BazelGlobalFunctionArgumentReference(this)) } ?: emptyList()
  }

  override fun getReference(element: StarlarkGlobExpression): PsiReference {
    return StarlarkGlobReference(element)
  }

  override fun getReference(element: StarlarkStringLiteralExpression): PsiReference? {
    with(element) {
      if (isInVisibilityList()) return StarlarkVisibilityReference(this)
      if (isLoadFilenameValue()) return BazelLabelReference(this, true)
      if (isUseExtensionArgument()) {
        val loadedFileNamePsi = useExtensionLoadedFileName() ?: return null
        val loadedFileReference = BazelLabelReference(loadedFileNamePsi, true)
        val reference = when (loadedFileNamePsi) {
          this -> loadedFileReference
          else -> StarlarkLoadReference(this, loadedFileReference)
        }
        return reference
      }
      val loadAncestor = findLoadStatement() ?: return BazelLabelReference(this, true)
      val loadedFileNamePsi = loadAncestor.getLoadedFileNamePsi() ?: return null
      val loadedFileReference = BazelLabelReference(loadedFileNamePsi, true)
      return when (loadedFileNamePsi) {
        this -> loadedFileReference
        else -> StarlarkLoadReference(this, loadedFileReference)
      }
    }
  }

  override fun getOwnReferences(element: StarlarkCallExpression): Collection<PsiSymbolReference> {
    with(element) {
      val name = getCalledFunctionName() ?: return emptyList()
      val function = BazelGlobalFunctions.getFunctionByName(name, project) ?: return emptyList()
      return listOfNotNull(
        BazelGlobalFunctionReference(this, function),
        getCalledExpression()?.reference?.let { PsiSymbolService.getInstance().asSymbolReference(it) },
      )
    }
  }
}

private fun StarlarkReferenceExpression.isBeforeDot(): Boolean =
  generateSequence(node.treeNext) { it.treeNext }.any { it.elementType == StarlarkTokenTypes.DOT }

private fun StarlarkReferenceExpression.isThrowaway(): Boolean = name == "_"

private fun StarlarkStringLiteralExpression.isLoadFilenameValue(): Boolean = parent is StarlarkFilenameLoadValue

private fun StarlarkStringLiteralExpression.findLoadStatement(): StarlarkLoadStatement? = (parent as? StarlarkLoadValue)?.getLoadStatement()

private fun StarlarkStringLiteralExpression.isInVisibilityList(): Boolean =
  (
    parent is StarlarkListLiteralExpression && (parent.parent as? StarlarkNamedArgumentExpression)?.name in
      listOf(
        "visibility",
        "default_visibility",
      )
  )
