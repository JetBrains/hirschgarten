package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionsService
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkHighlightingColors
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.references.StarlarkNamedArgumentReference

class StarlarkFunctionAnnotator : StarlarkAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    when {
      isFunctionDeclaration(element) -> holder.mark(element, StarlarkHighlightingColors.FUNCTION_DECLARATION)
      isNamedArgument(element) -> annotateNamedArgument(element, holder)
      isGlobalFunction(element) -> annotateGlobalFunction(element, holder)
      else -> {}
    }
  }

  private fun isFunctionDeclaration(element: PsiElement): Boolean =
    checkElementAndParentType(
      element = element,
      expectedElementTypes = listOf(StarlarkTokenTypes.IDENTIFIER),
      expectedParentTypes = listOf(StarlarkElementTypes.FUNCTION_DECLARATION),
    )

  private fun isGlobalFunction(element: PsiElement): Boolean =
    checkElementAndParentType(
      element = element,
      expectedElementTypes = listOf(StarlarkElementTypes.CALL_EXPRESSION),
      expectedParentTypes = listOf(StarlarkElementTypes.EXPRESSION_STATEMENT),
    ) &&
      (element.containingFile as? StarlarkFile)?.getBazelFileType()?.let { it == BazelFileType.BUILD || it == BazelFileType.MODULE } == true

  private fun annotateGlobalFunction(element: PsiElement, holder: AnnotationHolder) {
    if (element.firstChild == null) return
    val functionName = element.firstChild.text
    holder.mark(element.firstChild, StarlarkHighlightingColors.FUNCTION_DECLARATION)

    val function = BazelGlobalFunctionsService.getInstance().getFunctionByName(functionName)
    if (function != null) {
      val argumentList = element.lastChild
      val arguments = argumentList as? StarlarkArgumentList ?: return
      val argumentNames = arguments.getArgumentNames()
      val requiredArguments = function.params.filter { it.required }.map { it.name }
      val missingArguments = requiredArguments.filter { !argumentNames.contains(it) }
      if (missingArguments.isNotEmpty()) {
        holder.annotateError(
          element = element.firstChild, // Only underline the function name.
          message = "Missing required arguments: " + missingArguments.joinToString(", "),
        )
      }
    }
  }

  private fun isNamedArgument(element: PsiElement): Boolean =
    checkElementAndParentType(
      element = element,
      expectedElementTypes = listOf(StarlarkTokenTypes.IDENTIFIER, StarlarkTokenTypes.EQ),
      expectedParentTypes = listOf(StarlarkElementTypes.NAMED_ARGUMENT_EXPRESSION),
    )

  private fun annotateNamedArgument(element: PsiElement, holder: AnnotationHolder) {
    if (isNotResolvedNamedArgument(element)) {
      if (element.elementType == StarlarkTokenTypes.IDENTIFIER) {
        holder.annotateError(element, StarlarkBundle.message("annotator.named.parameter.not.found", element.text))
      }
    } else {
      holder.mark(element, StarlarkHighlightingColors.NAMED_ARGUMENT)
    }
  }

  private fun isNotResolvedNamedArgument(element: PsiElement): Boolean {
    val reference = (element.parent.reference as? StarlarkNamedArgumentReference) ?: return false
    return reference.resolveFunction() != null && reference.resolve() == null
  }
}
