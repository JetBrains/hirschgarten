package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.components.service
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.modules.BazelModuleRegistryService
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
    val project = parameters.editor.project ?: return
    val stringLiteral = parameters.position.parentOfType<StarlarkStringLiteralExpression>() ?: return
    val callExpression = stringLiteral.parentOfType<StarlarkCallExpression>() ?: return

    if (getFunctionName(callExpression) != "bazel_dep") return

    val argumentName = getArgumentName(stringLiteral, callExpression) ?: return
    val registryService = BazelModuleRegistryService.getInstance(project)

    when (argumentName) {
      "name" -> {
        val cachedNames = registryService.getCachedModuleNames()
        if (cachedNames.isNotEmpty()) {
          result.addAllElements(
            cachedNames.map { name: String ->
              getCompletionLookupElemenent(name, PlatformIcons.PACKAGE_ICON, 1.0)
            }
          )
        }
        project.service<BazelCoroutineService>().start {
          registryService.getModuleNames()
        }
      }
      "version" -> {
        //result.addElement(
        //  getCompletionLookupElemenent("latest", PlatformIcons.PROPERTY_ICON, 1.0),
        //)
        val moduleName = getModuleName(callExpression)
        if (moduleName != null) {
          val cachedVersions = registryService.getCachedModuleVersions(moduleName)
          if (cachedVersions.isNotEmpty()) {
            result.addAllElements(
              cachedVersions.map { version: String ->
                getCompletionLookupElemenent(version, PlatformIcons.PROPERTY_ICON, getVersionPriority(version))
              }
            )
          }
          project.service<BazelCoroutineService>().start {
            registryService.getModuleVersions(moduleName)
          }
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

  private fun getVersionPriority(version: String): Double {
    return runCatching {
      version.split(".").joinToString("").toDouble() / 1000
    }.getOrDefault(0.0)
  }
}
