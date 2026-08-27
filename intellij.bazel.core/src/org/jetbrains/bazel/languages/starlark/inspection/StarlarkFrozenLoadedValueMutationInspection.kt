package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue
import org.jetbrains.bazel.languages.starlark.utils.StarlarkMutationUtils

@ApiStatus.Internal
class StarlarkFrozenLoadedValueMutationInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = FrozenLoadedValueMutationVisitor(holder)

  private class FrozenLoadedValueMutationVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    private val reported = mutableSetOf<PsiElement>()

    override fun visitFile(psiFile: PsiFile) {
      val file = psiFile as? StarlarkFile ?: return
      val loadedBindings = collectLoadedLocalBindings(file)
      if (loadedBindings.isEmpty()) return

      val registry = LoadedBindingsRegistry(loadedBindings)

      PsiTreeUtil.processElements(file) {
        val mutation = StarlarkMutationUtils.mutationOrNull(it) ?: return@processElements true
        val loadedBinding = registry.find(mutation.target) ?: return@processElements true

        if (reported.add(mutation.problemElement)) {
          holder.registerProblem(
            mutation.problemElement,
            StarlarkBundle.message("inspection.description.loaded.value.mutation", loadedBinding.name),
          )
        }

        true
      }
    }

    private data class LoadedBinding(val element: PsiElement, val name: String)

    private class LoadedBindingsRegistry(bindings: List<LoadedBinding>) {
      private val byName: Map<String, LoadedBinding> = bindings.associateBy { it.name }
      private val byElement: Map<PsiElement, LoadedBinding> = buildMap {
        for (binding in bindings) {
          put(binding.element, binding)
          put(binding.element.parent, binding)
        }
      }

      fun find(element: PsiElement): LoadedBinding? {
        val resolved = element.reference?.resolve()
        if (resolved != null) {
          byElement[resolved]?.let { return it }
          if (resolved.containingFile == element.containingFile) return null
        }

        val name = StarlarkMutationUtils.referenceName(element) ?: return null
        return byName[name]
      }
    }

    private fun collectLoadedLocalBindings(file: StarlarkFile): List<LoadedBinding> =
      file.children
        .filterIsInstance<StarlarkLoadStatement>()
        .flatMap { it.getLoadedSymbolsPsi().filterIsInstance<StarlarkLoadValue>() }
        .mapNotNull(::loadedLocalBinding)

    private fun loadedLocalBinding(value: StarlarkLoadValue): LoadedBinding? =
      when (value) {
        is StarlarkNamedLoadValue -> {
          val name = value.name ?: return null
          LoadedBinding(value.nameIdentifier ?: value, name)
        }
        is StarlarkStringLoadValue -> {
          val name = value.getLoadValueExpressionContent() ?: return null
          LoadedBinding(value.getLoadValueExpression() ?: value, name)
        }
        else -> null
      }
  }
}
