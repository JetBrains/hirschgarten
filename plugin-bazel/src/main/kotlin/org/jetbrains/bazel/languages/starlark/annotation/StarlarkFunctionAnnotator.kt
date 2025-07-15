package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionParameter
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionsService
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkHighlightingColors
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
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
      doAnnotateGlobalFunction(function, element, holder)
      if (function.name == "git_override" || function.name == "archive_override") {
        checkDependencyOverrideResolution(element, holder)
      }
    }
  }

  private fun doAnnotateGlobalFunction(
    function: BazelGlobalFunction,
    element: PsiElement,
    holder: AnnotationHolder,
  ) {
    val arguments = element.lastChild as? StarlarkArgumentList ?: return
    val params = function.params
    val expectedParamIter = params.listIterator()
    var expectedParam = expectedParamIter.nextOrNull()
    var onlyKeywordArgs = false
    val matchedArguments = mutableSetOf<String>()
    val acceptsKwArgs = params.any { it.isKwArgs() }
    var kwArgsFound = false
    for (child in arguments.children) {
      when (child) {
        is StarlarkNamedArgumentExpression -> {
          val argName = child.name ?: continue
          if (!params.any { it.name == argName } && !acceptsKwArgs) {
            // Cannot be resolved
            holder.annotateError(child, StarlarkBundle.message("annotator.named.parameter.not.found", argName))
          } else if (matchedArguments.contains(argName)) {
            // Duplicate argument
            holder.annotateError(child, StarlarkBundle.message("annotator.duplicate.keyword.argument", argName))
          } else {
            matchedArguments.add(argName)
            if (!params.any { it.name == argName }) {
              kwArgsFound = true
            }
          }
          onlyKeywordArgs = true
        }
        is StarlarkArgumentExpression -> {
          if (onlyKeywordArgs) {
            holder.annotateError(child, StarlarkBundle.message("annotator.positional.argument.after.keyword.argument"))
          } else if (expectedParam == null || expectedParam.isKwArgs()) {
            holder.annotateError(child, StarlarkBundle.message("annotator.too.many.positional.arguments"))
          } else {
            val argName = expectedParam.name
            if (!matchedArguments.contains(argName)) {
              matchedArguments.add(argName)
            }
            if (!expectedParam.isVarArgs()) {
              expectedParam = expectedParamIter.nextOrNull()
            }
          }
        }
      }
    }

    val missingArguments = mutableListOf<String>()
    for (param in params) {
      if ((param.required && !matchedArguments.contains(param.name) && !param.isKwArgs()) ||
        (param.required && param.isKwArgs() && !kwArgsFound)
      ) {
        missingArguments.add(param.name)
      }
    }
    if (missingArguments.isNotEmpty()) {
      holder.annotateError(
        element = arguments,
        message = StarlarkBundle.message("annotator.missing.required.args", missingArguments.joinToString(", ")),
      )
    }
  }

  private fun ListIterator<BazelGlobalFunctionParameter>.nextOrNull() = if (hasNext()) next() else null

  private fun BazelGlobalFunctionParameter.isKwArgs() = name.startsWith("**")

  private fun BazelGlobalFunctionParameter.isVarArgs() = name.startsWith("*")

  private fun checkDependencyOverrideResolution(element: PsiElement, holder: AnnotationHolder) {
    val reference = (element as? StarlarkCallExpression)?.reference ?: return
    if (reference.resolve() == null) {
      holder.annotateError(
        element = element.firstChild,
        message = StarlarkBundle.message("annotator.override.missing.dep"),
      )
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
