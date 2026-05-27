package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue


@ApiStatus.Internal
class StarlarkLoadDuplicateSymbolsInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = DuplicateLoadSymbolsVisitor(holder)

  private class DuplicateLoadSymbolsVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitLoadStatement(node: StarlarkLoadStatement) {
      val seenLocalNames = mutableSetOf<String>()
      val loadValues = node.getLoadedSymbolsPsi().filterIsInstance<StarlarkLoadValue>().filterNot { it is StarlarkFilenameLoadValue }

      for (value in loadValues) {
        val binding = extractBinding(value) ?: continue

        if (!seenLocalNames.add(binding.name)) {
          holder.registerProblem(
            binding.element,
            StarlarkBundle.message("inspection.description.load.name.defined.more.than.once")
          )
        }
      }
    }

    private fun extractBinding(value: StarlarkLoadValue): LoadBinding? = when (value) {
      is StarlarkStringLoadValue -> {
        val localPsi = value.getLoadValueExpression() ?: return null
        val localName = localPsi.getStringContents()
        LoadBinding(localName, localPsi)
      }
      is StarlarkNamedLoadValue -> {
        val localPsi = value.nameIdentifier ?: value
        val localName = value.name ?: return null
        LoadBinding(localName, localPsi)
      }
      else -> null
    }
  }

  private data class LoadBinding(
    val name: String,
    val element: PsiElement
  )
}
