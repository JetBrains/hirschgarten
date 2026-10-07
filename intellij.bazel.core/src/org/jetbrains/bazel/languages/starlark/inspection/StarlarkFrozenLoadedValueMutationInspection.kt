package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFileLoadedSymbols
import org.jetbrains.bazel.languages.starlark.utils.StarlarkLoadedSymbol
import org.jetbrains.bazel.languages.starlark.utils.StarlarkMutationUtils

@ApiStatus.Internal
class StarlarkFrozenLoadedValueMutationInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = FrozenLoadedValueMutationVisitor(holder)

  private class FrozenLoadedValueMutationVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitFile(psiFile: PsiFile) {
      val file = psiFile as? StarlarkFile ?: return
      val loadedSymbols = StarlarkFileLoadedSymbols.create(file)
      if (loadedSymbols.isEmpty) return

      val reported = mutableSetOf<PsiElement>()

      PsiTreeUtil.processElements(file) {
        val mutation = StarlarkMutationUtils.mutationOrNull(it) ?: return@processElements true
        val loadedSymbol = loadedSymbols.findByReference(mutation.target) ?: return@processElements true
        val problemDescription = mutationProblemDescription(mutation, loadedSymbol) ?: return@processElements true

        if (reported.add(mutation.problemElement)) {
          holder.registerProblem(mutation.problemElement, problemDescription)
        }

        true
      }
    }

    @InspectionMessage
    private fun mutationProblemDescription(mutation: StarlarkMutationUtils.Mutation, loadedSymbol: StarlarkLoadedSymbol): String? =
      when (mutation.kind) {
        StarlarkMutationUtils.Mutation.Kind.SUBSCRIPTION_ASSIGNMENT,
        StarlarkMutationUtils.Mutation.Kind.AUGMENTED_ASSIGNMENT ->
          StarlarkBundle.message("inspection.description.loaded.value.mutation", loadedSymbol.localName)

        StarlarkMutationUtils.Mutation.Kind.METHOD_CALL -> {
          val methodName = mutation.methodName ?: return null
          val valueKind = loadedSymbol.staticValueKind ?: return null
          if (!valueKind.isMutatedBy(methodName)) return null
          StarlarkBundle.message("inspection.description.loaded.value.mutation", loadedSymbol.localName)
        }
      }
  }
}
