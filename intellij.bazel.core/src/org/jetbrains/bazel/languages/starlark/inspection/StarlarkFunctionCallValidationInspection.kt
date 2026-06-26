package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.bazel.Environment
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkStarArgumentExpression
import org.jetbrains.bazel.languages.starlark.utils.StarlarkCallableFunctionInfoProvider
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFunctionInfoProvider
import org.jetbrains.bazel.languages.starlark.utils.StarlarkGlobalFunctionInfoProvider
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFuncParam


@ApiStatus.Internal
class StarlarkFunctionCallValidationInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = CallArgumentValidationVisitor(holder)

  private class CallArgumentValidationVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    private val signatureProviders: List<StarlarkFunctionInfoProvider> = listOf(
      StarlarkGlobalFunctionInfoProvider(),
      StarlarkCallableFunctionInfoProvider(),
    )

    override fun visitCallExpression(node: StarlarkCallExpression) {
      val arguments = node.getArgumentList()?.getArguments() ?: return
      validateOrder(arguments)

      val resolved = signatureProviders.firstNotNullOfOrNull { it.resolve(node) } ?: return
      if (!validateEnvironment(node, resolved.allowedEnvironments)) return
      validateAgainstSignature(node, arguments, resolved.params)
      checkDependencyOverrideResolution(node)
    }

    private fun validateEnvironment(call: StarlarkCallExpression, allowedEnvironments: Set<Environment>?): Boolean {
      if (allowedEnvironments == null) return true

      val currentEnvironment = when (val fileName = call.containingFile.name) {
        Constants.MODULE_BAZEL_FILE_NAME -> Environment.MODULE
        in Constants.BUILD_FILE_NAMES -> Environment.BUILD
        in Constants.WORKSPACE_FILE_NAMES -> Environment.BUILD
        "REPO.bazel" -> Environment.REPO
        "VENDOR.bazel" -> Environment.VENDOR
        else -> if (fileName.endsWith(".bzl")) Environment.BZL else null
      } ?: return true
      if (currentEnvironment in allowedEnvironments) return true

      holder.registerProblem(
        call.firstChild ?: call,
        StarlarkBundle.message(
          "inspection.description.call.not.available.in.environment",
          call.firstChild?.text ?: "<call>", currentEnvironment.name, allowedEnvironments.map { it.name }.sorted().joinToString(", "),
        )
      )
      return false
    }

    private fun checkDependencyOverrideResolution(call: StarlarkCallExpression) {
      val functionName = call.firstChild?.text ?: return
      if (functionName != "git_override" && functionName != "archive_override") return
      val reference = call.getCalledExpression()?.reference ?: return
      if (reference.resolve() != null) return
      holder.registerProblem(
        call.firstChild,
        StarlarkBundle.message("inspection.description.call.override.missing.dep"),
      )
    }

    private fun validateOrder(arguments: Array<StarlarkArgumentElement>) {
      var seenNamed = false
      var seenStar = false
      var seenKwStar = false

      for (arg in arguments) {
        when (arg) {
          is StarlarkArgumentExpression -> {
            if (seenNamed || seenStar || seenKwStar) holder.registerProblem(arg, StarlarkBundle.message("inspection.description.call.positional.after.keyword.or.star"))
          }

          is StarlarkNamedArgumentExpression -> {
            if (seenKwStar) holder.registerProblem(arg, StarlarkBundle.message("inspection.description.call.argument.after.kwargs"))
            seenNamed = true
          }

          is StarlarkStarArgumentExpression -> {
            if (isKwStar(arg)) {
              if (seenKwStar) holder.registerProblem(arg, StarlarkBundle.message("inspection.description.call.multiple.kwargs.unpacking"))
              seenKwStar = true
            } else {
              if (seenKwStar) holder.registerProblem(arg, StarlarkBundle.message("inspection.description.call.star.after.kwargs"))
              seenStar = true
            }
          }
        }
      }
    }

    private fun validateAgainstSignature(
      call: StarlarkCallExpression,
      arguments: Array<StarlarkArgumentElement>,
      params: List<StarlarkFuncParam>,
    ) {
      val expectedParamIter = params.filter { it.positional }.listIterator()
      var expectedParam = expectedParamIter.nextOrNull()
      var onlyKeywordArgsExpected = false
      var hasUnknownPositional = false
      var hasUnknownKeywords = false
      var kwArgsFound = false

      val matchedArguments = linkedSetOf<String>()
      val matchedByPositional = mutableSetOf<String>()
      val acceptsKwArgs = params.any { it.isKwArgs }

      for (arg in arguments) {
        when (arg) {
          is StarlarkNamedArgumentExpression -> {
            val argName = arg.name ?: continue
            val matched = params.firstOrNull { it.name == argName }

            when {
              matched == null && !acceptsKwArgs ->
                holder.registerProblem(
                  arg.firstChild,
                  StarlarkBundle.message("inspection.description.call.named.parameter.not.found", argName)
                )

              argName in matchedArguments ->
                holder.registerProblem(
                  arg,
                  StarlarkBundle.message(
                    if (argName in matchedByPositional) {
                      "inspection.description.call.multiple.values.for.parameter"
                    } else {
                      "inspection.description.call.duplicate.named.argument"
                    },
                    argName,
                  ),
                )

              matched != null && !matched.named ->
                holder.registerProblem(arg, StarlarkBundle.message("inspection.description.call.unnamed.arg.with.name", argName))

              else -> {
                matchedArguments.add(argName)
                if (matched == null) kwArgsFound = true
              }
            }

            onlyKeywordArgsExpected = true
          }

          is StarlarkArgumentExpression -> {
            if (onlyKeywordArgsExpected) continue

            if (expectedParam == null) {
              holder.registerProblem(arg, StarlarkBundle.message("inspection.description.call.too.many.positional"))
              continue
            }

            if (expectedParam.name !in matchedArguments) {
              matchedArguments.add(expectedParam.name)
              matchedByPositional.add(expectedParam.name)
            }
            if (!expectedParam.isVarArgs) expectedParam = expectedParamIter.nextOrNull()
          }

          is StarlarkStarArgumentExpression -> {
            onlyKeywordArgsExpected = true
            if (isKwStar(arg)) hasUnknownKeywords = true else hasUnknownPositional = true
          }
        }
      }

      val missingArguments = mutableListOf<String>()
      for (param in params) {
        if (!param.required) continue
        if (param.name in matchedArguments) continue
        if (param.isKwArgs && kwArgsFound) continue
        if (param.positional && hasUnknownPositional) continue
        if (param.named && hasUnknownKeywords) continue

        missingArguments.add(param.name)
      }
      if (missingArguments.isNotEmpty()) {
        holder.registerProblem(
          call.firstChild, // only annotate the function name
          StarlarkBundle.message("inspection.description.call.missing.required.argument", missingArguments.joinToString(", ")),
        )
      }
    }

    private fun isKwStar(arg: StarlarkStarArgumentExpression): Boolean = arg.text.trimStart().startsWith("**")

    private fun <T> ListIterator<T>.nextOrNull(): T? = if (hasNext()) next() else null
  }
}
