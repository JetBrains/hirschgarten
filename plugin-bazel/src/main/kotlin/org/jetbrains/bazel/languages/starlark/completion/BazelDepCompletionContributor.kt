package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.bzlmod.BazelModuleRegistryService
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.getCompletionLookupElemenent

class BazelDepCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      psiElement()
        .withLanguage(StarlarkLanguage)
        .inside(StarlarkStringLiteralExpression::class.java)
        .inside(StarlarkCallExpression::class.java),
      BazelDepCompletionProvider(),
    )
  }
}

class BazelDepCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val stringLiteral = parameters.position.parentOfType<StarlarkStringLiteralExpression>() ?: return
    val callExpression = stringLiteral.parentOfType<StarlarkCallExpression>() ?: return
    if (getFunctionName(callExpression) != "bazel_dep") return

    val project = parameters.editor.project ?: return
    val registryService = BazelModuleRegistryService.getInstance(project)
    result.restartCompletionWhenNothingMatches()

    val argumentName = getArgumentName(stringLiteral, callExpression) ?: return

    runBlockingCancellable {
      when (argumentName) {
        "name" -> {
          val names = registryService.getModuleNames()
          names.forEach { name ->
            result.addElement(getCompletionLookupElemenent(name, PlatformIcons.PACKAGE_ICON, 1.0))
          }
        }
        "version" -> {
          val moduleName = getModuleName(callExpression) ?: return@runBlockingCancellable
          val versions = registryService.getModuleVersions(moduleName)
          result.addAllElements(
            versions.mapIndexed { index, v ->
              getCompletionLookupElemenent(v, PlatformIcons.PROPERTY_ICON, (-index).toDouble())
            },
          )
        }
      }
    }
  }

  private fun getFunctionName(callExpression: StarlarkCallExpression): String? = callExpression.getNamePsi()?.text

  private fun getArgumentName(stringLiteral: StarlarkStringLiteralExpression, callExpression: StarlarkCallExpression): String? {
    val arguments = callExpression.getArgumentList()?.getArguments() ?: return null
    for (arg in arguments) {
      if (arg.name != null && arg.getValue() == stringLiteral) {
        return arg.name
      }
    }
    return null
  }

  private fun getModuleName(callExpression: StarlarkCallExpression): String? {
    val nameArg = callExpression.getArgumentList()?.getKeywordArgument("name") ?: return null
    val stringLiteral = nameArg.getValue() as? StarlarkStringLiteralExpression ?: return null
    return stringLiteral.text.removeSurrounding("\"")
  }
}
