package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val stringLiteral = parameters.position.parentOfType<StarlarkStringLiteralExpression>() ?: return
    val callExpression = stringLiteral.parentOfType<StarlarkCallExpression>() ?: return
    if (getFunctionName(callExpression) != "bazel_dep") return

    val project = parameters.editor.project ?: return
    val registryService = BazelModuleRegistryService.getInstance(project)

    val argumentName = getArgumentName(stringLiteral, callExpression) ?: return
    when (argumentName) {
      "name" -> {
        val cachedNames = registryService.getCachedModuleNames()
        if (cachedNames.isNotEmpty()) {
          result.addAllElements(
            cachedNames.map { name: String ->
              getCompletionLookupElemenent(name, PlatformIcons.PACKAGE_ICON, 1.0)
            },
          )
        } else {
          BazelCoroutineService.getInstance(project).start {
            registryService.getModuleNames()
            invokeCompletion(project, parameters.editor)
          }
        }
      }
      "version" -> {
        val moduleName = getModuleName(callExpression) ?: return
        val cachedVersions = registryService.getCachedModuleVersions(moduleName)
        if (cachedVersions.isNotEmpty()) {
          result.addAllElements(
            cachedVersions.mapIndexed { index, version ->
              getCompletionLookupElemenent(version, PlatformIcons.PROPERTY_ICON, (-index).toDouble())
            },
          )
        } else {
          BazelCoroutineService.getInstance(project).start {
            registryService.getModuleVersions(moduleName)
            invokeCompletion(project, parameters.editor)
          }
        }
      }
    }
  }

  private suspend fun invokeCompletion(project: Project, editor: Editor) {
    withContext(Dispatchers.EDT) {
      if (project.isDisposed || editor.isDisposed) return@withContext
      CodeCompletionHandlerBase(CompletionType.BASIC, true, false, true)
        .invokeCompletion(project, editor)
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
