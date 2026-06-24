package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.bazel.Environment
import org.jetbrains.bazel.languages.starlark.bazel.isKwArgs
import org.jetbrains.bazel.languages.starlark.bazel.isVarArgs
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordOnlyBoundary
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkMandatoryParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkOptionalParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue


@ApiStatus.Internal
interface StarlarkFunctionInfoProvider {
  fun resolve(call: StarlarkCallExpression): StarlarkFunctionInfo?
}

@ApiStatus.Internal
data class StarlarkFunctionInfo(
  val params: List<StarlarkFuncParam>,
  val allowedEnvironments: Set<Environment>? = null,
)

@ApiStatus.Internal
data class StarlarkFuncParam(
  val name: String,
  val positional: Boolean,
  val named: Boolean,
  val required: Boolean,
  val isVarArgs: Boolean,
  val isKwArgs: Boolean,
)


@ApiStatus.Internal
class StarlarkGlobalFunctionInfoProvider : StarlarkFunctionInfoProvider {
  override fun resolve(call: StarlarkCallExpression): StarlarkFunctionInfo? {
    if (call.parent?.parent !is StarlarkFile) return null
    if (call.resolvesToFunctionDeclaration()) return null

    val functionName = call.firstChild?.text ?: return null
    val function = BazelGlobalFunctions.getFunctionByName(functionName, call.project) ?: return null
    val params = function.params.map {StarlarkFuncParam(it.name, it.positional, it.named, it.required, it.isVarArgs(), it.isKwArgs())}
    val environment = function.environment.toSet()

    return StarlarkFunctionInfo(params, environment)
  }

  private fun StarlarkCallExpression.resolvesToFunctionDeclaration(): Boolean {
    val calledExpression = getCalledExpression() as? StarlarkReferenceExpression ?: return false
    return calledExpression.reference?.resolve() is StarlarkFunctionDeclaration
  }
}

@ApiStatus.Internal
class StarlarkCallableFunctionInfoProvider : StarlarkFunctionInfoProvider {
  override fun resolve(call: StarlarkCallExpression): StarlarkFunctionInfo? {
    val expression = call.getCalledExpression() ?: return null

    findLambdaExpression(expression)?.let { return StarlarkFunctionInfo(getSignature(it)) }

    val refExpr = findReferenceExpression(expression) ?: return null
    val resolved = refExpr.reference?.resolve() ?: return null
    val callable = getCallableFromResolvedReference(resolved) ?: return null

    return StarlarkFunctionInfo(getSignature(callable))
  }

  private fun findLambdaExpression(expression: PsiElement): StarlarkLambdaExpression? =
    PsiTreeUtil.getChildOfType(expression, StarlarkLambdaExpression::class.java)

  private fun findReferenceExpression(expression: PsiElement): StarlarkReferenceExpression? =
    (expression as? StarlarkReferenceExpression) ?:
    PsiTreeUtil.getChildOfType(expression, StarlarkReferenceExpression::class.java)

  private fun getCallableFromResolvedReference(resolved: PsiElement): StarlarkCallable? =
    when (resolved) {
      is StarlarkCallable -> resolved
      is StarlarkTargetExpression -> resolveLambdaAlias(resolved)
      is StarlarkNamedLoadValue -> resolveLoadedCallable(resolved)
      else -> null
    }

  private fun resolveLambdaAlias(target: StarlarkTargetExpression): StarlarkCallable? {
    val assignment = PsiTreeUtil.getParentOfType(target, StarlarkAssignmentStatement::class.java, false) ?: return null
    return findLambdaExpression(assignment)
  }

  private fun resolveLoadedCallable(loadValue: StarlarkNamedLoadValue): StarlarkCallable? {
    val loaded = PsiTreeUtil.getChildOfType(loadValue, StarlarkStringLiteralExpression::class.java) ?: return null
    return loaded.reference?.resolve() as? StarlarkCallable
  }

  private fun getSignature(callable: StarlarkCallable): List<StarlarkFuncParam> {
    val parameterList = PsiTreeUtil.getChildOfType(callable, StarlarkParameterList::class.java)
    val signatureItems = parameterList?.children?.filter { it is StarlarkParameter || it is StarlarkKeywordOnlyBoundary }.orEmpty()

    var keywordOnly = false
    val params = mutableListOf<StarlarkFuncParam>()

    for (item in signatureItems) {
      when (item) {
        is StarlarkKeywordOnlyBoundary -> {
          keywordOnly = true
        }
        is StarlarkVariadicParameter -> {
          val name = item.name ?: continue
          params.add(StarlarkFuncParam(name, positional = true, named = false, required = false, isVarArgs = true, isKwArgs = false))
          keywordOnly = true
        }
        is StarlarkKeywordVariadicParameter -> {
          val name = item.name ?: continue
          params.add(StarlarkFuncParam(name, positional = false, named = true, required = false, isVarArgs = false, isKwArgs = true))
        }
        is StarlarkMandatoryParameter -> {
          val name = item.name ?: continue
          params.add(StarlarkFuncParam(name, positional = !keywordOnly, named = true, required = true, isVarArgs = false, isKwArgs = false))
        }
        is StarlarkOptionalParameter -> {
          val name = item.name ?: continue
          params.add(StarlarkFuncParam(name, positional = !keywordOnly, named = true, required = false, isVarArgs = false, isKwArgs = false))
        }
      }
    }

    return params
  }
}
