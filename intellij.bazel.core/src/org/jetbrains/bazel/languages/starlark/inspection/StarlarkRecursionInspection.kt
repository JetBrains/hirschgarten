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
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.utils.StarlarkCallableResolver

@ApiStatus.Internal
class StarlarkRecursionInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = RecursionVisitor(holder)

  private class RecursionVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    private val reported = mutableSetOf<PsiElement>()

    override fun visitFile(psiFile: PsiFile) {
      val file = psiFile as? StarlarkFile ?: return
      val callGraph = StarlarkCallGraph.build(file)

      callGraph.recursiveCalls().forEach { call ->
        val element = call.firstChild ?: call
        if (reported.add(element)) {
          holder.registerProblem(
            element,
            StarlarkBundle.message("inspection.description.recursion.detected"),
          )
        }
      }
    }
  }

  private class StarlarkCallGraph private constructor(
    private val resolvedCallsByCallable: Map<StarlarkCallable, List<ResolvedCall>>,
    private val cyclicComponentByCallable: Map<StarlarkCallable, Int>,
  ) {
    data class ResolvedCall(val call: StarlarkCallExpression, val target: StarlarkCallable)

    fun recursiveCalls(): List<StarlarkCallExpression> = resolvedCallsByCallable.flatMap { (callable, resolvedCalls) ->
      val sourceComponent = cyclicComponentByCallable[callable]
      resolvedCalls.filter { sourceComponent != null && cyclicComponentByCallable[it.target] == sourceComponent }.map { it.call }
    }

    companion object {
      fun build(file: StarlarkFile): StarlarkCallGraph {
        val callsByCallable = collectCallsByCallable(file)
        val callables = callsByCallable.keys
        val resolvedCallsByCallable = callsByCallable.mapValues { (_, calls) ->
          calls.mapNotNull {
            val target = StarlarkCallableResolver.resolve(it) ?: return@mapNotNull null
            if (target.containingFile != file) return@mapNotNull null
            ResolvedCall(it, target)
          }
        }
        val edges = resolvedCallsByCallable.mapValues { (_, calls) -> calls.mapTo(linkedSetOf()) { it.target } }

        return StarlarkCallGraph(
          resolvedCallsByCallable,
          findCyclicComponentByCallable(callables, edges)
        )
      }

      private fun collectCallsByCallable(file: StarlarkFile): Map<StarlarkCallable, List<StarlarkCallExpression>> {
        val callsByCallable = linkedMapOf<StarlarkCallable, MutableList<StarlarkCallExpression>>()

        PsiTreeUtil.processElements(file) { element ->
          when (element) {
            is StarlarkCallable -> callsByCallable[element] = mutableListOf()
            is StarlarkCallExpression -> {
              val owner = PsiTreeUtil.getParentOfType(element, StarlarkCallable::class.java, true)
              if (owner != null) callsByCallable.getOrPut(owner) { mutableListOf() }.add(element)
            }
          }
          true
        }

        return callsByCallable
      }

      // In the call graph, recursion is represented by a cyclic strongly connected component
      // (several callables that can reach each other or a single callable with a self-call).
      private fun findCyclicComponentByCallable(
        callables: Collection<StarlarkCallable>,
        edges: Map<StarlarkCallable, Set<StarlarkCallable>>,
      ): Map<StarlarkCallable, Int> =
        stronglyConnectedComponents(callables, edges)
          .filter { isCyclicComponent(it, edges) }
          .flatMapIndexed { index, component -> component.map { it to index } }
          .toMap()

      private fun isCyclicComponent(component: List<StarlarkCallable>, edges: Map<StarlarkCallable, Set<StarlarkCallable>>): Boolean {
        if (component.size > 1) return true
        val callable = component.singleOrNull() ?: return false
        return callable in edges[callable].orEmpty()
      }

      // Tarjan's algorithm for precomputing strongly connected components.
      private fun stronglyConnectedComponents(
        callables: Collection<StarlarkCallable>,
        edges: Map<StarlarkCallable, Set<StarlarkCallable>>,
      ): List<List<StarlarkCallable>> {
        var nextIndex = 0
        val indexByCallable = mutableMapOf<StarlarkCallable, Int>()
        val lowLinkByCallable = mutableMapOf<StarlarkCallable, Int>()
        val stack = ArrayDeque<StarlarkCallable>()
        val onStack = mutableSetOf<StarlarkCallable>()
        val components = mutableListOf<List<StarlarkCallable>>()

        fun visit(callable: StarlarkCallable) {
          indexByCallable[callable] = nextIndex
          lowLinkByCallable[callable] = nextIndex
          nextIndex++

          stack.addLast(callable)
          onStack.add(callable)

          for (target in edges[callable].orEmpty()) {
            if (target !in indexByCallable) {
              visit(target)
              lowLinkByCallable[callable] = minOf(lowLinkByCallable.getValue(callable), lowLinkByCallable.getValue(target))
            }
            else if (target in onStack) {
              lowLinkByCallable[callable] = minOf(lowLinkByCallable.getValue(callable), indexByCallable.getValue(target))
            }
          }

          if (lowLinkByCallable.getValue(callable) == indexByCallable.getValue(callable)) {
            val component = mutableListOf<StarlarkCallable>()
            do {
              val target = stack.removeLast()
              onStack.remove(target)
              component.add(target)
            }
            while (target != callable)

            components.add(component)
          }
        }

        callables.forEach { if (it !in indexByCallable) visit(it) }

        return components
      }
    }
  }
}
