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
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantAfter
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.relevantChildrenAfterEach
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListCompExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.utils.StarlarkMutationUtils

@ApiStatus.Internal
class StarlarkCollectionMutationDuringIterationInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = CollectionMutationVisitor(holder)

  private class CollectionMutationVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitForStatement(node: StarlarkForStatement) {
      val collection = nearestRelevantAfter(node, StarlarkTokenTypes.IN_KEYWORD) ?: return
      val body = node.getStatementLists().firstOrNull() ?: return
      reportMutationsOfIteratedCollections(listOf(collection), body)
    }

    override fun visitListCompExpression(node: StarlarkListCompExpression) = checkComprehension(node)

    override fun visitDictCompExpression(node: StarlarkDictCompExpression) = checkComprehension(node)

    private fun checkComprehension(node: StarlarkCompExpression) {
      val collections = relevantChildrenAfterEach(node, StarlarkTokenTypes.IN_KEYWORD)
      reportMutationsOfIteratedCollections(collections, node)
    }

    private fun reportMutationsOfIteratedCollections(collectionExpressions: List<PsiElement>, scope: PsiElement) {
      val iteratedCollections = collectionExpressions.filter { StarlarkMutationUtils.referenceName(it) != null }
      if (iteratedCollections.isEmpty()) return

      val reported = mutableSetOf<PsiElement>()
      PsiTreeUtil.processElements(scope) {
        if (it != scope && isInsideNestedCallable(it, scope)) return@processElements true

        val mutation = StarlarkMutationUtils.mutationOrNull(it) ?: return@processElements true
        val mutatesIteratedCollection = iteratedCollections.any { collection ->
          StarlarkMutationUtils.areReferencesEqual(mutation.target, collection)
        }
        if (!mutatesIteratedCollection) return@processElements true

        if (reported.add(mutation.problemElement)) {
          holder.registerProblem(
            mutation.problemElement,
            StarlarkBundle.message("inspection.description.collection.mutation.during.iteration"),
          )
        }

        true
      }
    }

    private fun isInsideNestedCallable(element: PsiElement, outer: PsiElement): Boolean {
      var current = element.parent
      while (current != null && current != outer) {
        if (current is StarlarkCallable) return true
        current = current.parent
      }
      return false
    }
  }
}
