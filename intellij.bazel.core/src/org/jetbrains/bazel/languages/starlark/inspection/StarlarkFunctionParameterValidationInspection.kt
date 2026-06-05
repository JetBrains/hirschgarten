package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordOnlyBoundary
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkMandatoryParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkOptionalParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter

@ApiStatus.Internal
class StarlarkFunctionParameterValidationInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    ParameterValidationVisitor(holder)

  private class ParameterValidationVisitor(
    private val holder: ProblemsHolder,
  ) : StarlarkElementVisitor() {

    override fun visitFunctionDeclaration(node: StarlarkFunctionDeclaration) = validateCallable(node)

    override fun visitLambdaExpression(node: StarlarkLambdaExpression) = validateCallable(node)

    private fun validateCallable(callable: StarlarkCallable) {
      val parameterList = PsiTreeUtil.getChildOfType(callable, StarlarkParameterList::class.java) ?: return
      val signatureItems = parameterList.children.filter { it is StarlarkParameter || it is StarlarkKeywordOnlyBoundary }
      if (signatureItems.isEmpty()) return

      val seenNames = mutableSetOf<String>()
      var state = ParameterState.BEFORE_OPTIONAL
      var boundarySeen = false

      for ((index, item) in signatureItems.withIndex()) {
        if (state == ParameterState.AFTER_KEYWORD_VARIADIC) {
          holder.registerProblem(
            item,
            StarlarkBundle.message("inspection.description.parameters.after.keyword.variadic"),
          )
          continue
        }

        when (item) {
          is StarlarkParameter -> {
            checkDuplicateName(item, seenNames)

            when (item) {
              is StarlarkMandatoryParameter -> {
                if (state == ParameterState.AFTER_OPTIONAL) {
                  holder.registerProblem(
                    item,
                    StarlarkBundle.message("inspection.description.parameters.mandatory.after.optional"),
                  )
                }
              }
              is StarlarkOptionalParameter -> {
                if (state == ParameterState.BEFORE_OPTIONAL) {
                  state = ParameterState.AFTER_OPTIONAL
                }
              }
              is StarlarkVariadicParameter -> {
                if (state == ParameterState.AFTER_VARIADIC) {
                  holder.registerProblem(
                    item,
                    StarlarkBundle.message("inspection.description.parameters.multiple.variadic"),
                  )
                }
                else {
                  state = ParameterState.AFTER_VARIADIC
                }
              }
              is StarlarkKeywordVariadicParameter -> {
                state = ParameterState.AFTER_KEYWORD_VARIADIC
              }
            }
          }

          is StarlarkKeywordOnlyBoundary -> {
            if (boundarySeen || state == ParameterState.AFTER_VARIADIC) {
              holder.registerProblem(
                item,
                StarlarkBundle.message("inspection.description.parameters.multiple.variadic"),
              )
              continue
            }

            boundarySeen = true
            state = ParameterState.AFTER_VARIADIC

            val nextItem = signatureItems.getOrNull(index + 1)
            val isValidFollower = nextItem is StarlarkMandatoryParameter || nextItem is StarlarkOptionalParameter
            if (!isValidFollower) {
              holder.registerProblem(
                item,
                StarlarkBundle.message("inspection.description.parameters.bare.asterisk"),
              )
            }
          }
        }
      }
    }

    private fun checkDuplicateName(parameter: StarlarkParameter, seenNames: MutableSet<String>) {
      val name = parameter.name ?: return
      if (!seenNames.add(name)) {
        holder.registerProblem(
          parameter,
          StarlarkBundle.message("inspection.description.parameters.duplicate.name", name),
        )
      }
    }
  }

  private enum class ParameterState {
    BEFORE_OPTIONAL,
    AFTER_OPTIONAL,
    AFTER_VARIADIC,
    AFTER_KEYWORD_VARIADIC,
  }
}
