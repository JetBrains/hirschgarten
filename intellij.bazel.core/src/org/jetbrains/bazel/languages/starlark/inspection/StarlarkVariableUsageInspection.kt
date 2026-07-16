package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

@ApiStatus.Internal
class StarlarkVariableUsageInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = VariableUsagesVisitor(holder)

  private class VariableUsagesVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    private val reported = mutableSetOf<PsiElement>()
    private var scopeIndex: StarlarkScopeIndex? = null
    private var knownGlobalNames: Set<String> = emptySet()

    override fun visitFile(psiFile: PsiFile) {
      val file = psiFile as? StarlarkFile ?: return
      scopeIndex = StarlarkScopeIndex.getOrBuild(file)
      knownGlobalNames = BazelGlobalFunctions.globalFunctions(file.project).keys
      collectReferences(file).forEach(::checkReference)
    }

    private fun checkReference(ref: StarlarkReferenceExpression) {
      val index = scopeIndex ?: return
      val name = ref.name ?: return
      if (!shouldAnalyzeReference(ref)) return

      when (val usage = index.analyzeReferenceUsage(ref)) {
        is StarlarkScopeIndex.RefUsage.BeforeAssignment -> {
          registerOnce(ref, StarlarkBundle.message("inspection.description.variable.referenced.before.assignment", usage.kind, name))
        }
        StarlarkScopeIndex.RefUsage.Bound -> return
        StarlarkScopeIndex.RefUsage.Unbound -> {
          ref.reference?.resolve()?.let { return }
          registerOnce(ref, StarlarkBundle.message("inspection.description.variable.undefined", name))
        }
      }
    }

    private fun collectReferences(file: StarlarkFile): Collection<StarlarkReferenceExpression> =
      PsiTreeUtil.collectElementsOfType(file, StarlarkReferenceExpression::class.java)

    private fun shouldAnalyzeReference(ref: StarlarkReferenceExpression): Boolean {
      val name = ref.name ?: return false
      if (name == "_") return false
      if (isAssignmentTarget(ref, name)) return false
      if (isQualifiedReference(ref)) return false
      if (name in knownGlobalNames) return false
      return true
    }

    private fun isAssignmentTarget(ref: StarlarkReferenceExpression, name: String): Boolean {
      val assignment = ref.parent as? StarlarkAssignmentStatement ?: return false
      return assignment.name == name
    }

    private fun isQualifiedReference(ref: StarlarkReferenceExpression): Boolean {
      if (ref.getQualifierExpression() != null) return true
      val parentRef = ref.parent as? StarlarkReferenceExpression
      return parentRef?.getQualifierExpression() == ref
    }

    private fun registerOnce(element: PsiElement, @InspectionMessage message: String) {
      if (reported.add(element)) holder.registerProblem(element, message)
    }
  }


  private class StarlarkScopeIndex private constructor(private val scopeDataByScope: Map<PsiElement, ScopeData>) {
    enum class ScopeKind { FILE, FUNCTION }

    data class ScopeData(
      val firstAssignmentByName: Map<String, Int>,
      val callOffsetsByFunctionName: Map<String, List<Int>>,
      val parentScope: PsiElement?,
      val kind: ScopeKind,
    ) {
      val isFunctionScope: Boolean get() = kind == ScopeKind.FUNCTION
      val label: String get() = when (kind) {
        ScopeKind.FILE -> GLOBAL
        ScopeKind.FUNCTION -> LOCAL
      }
    }

    sealed interface RefUsage {
      data class BeforeAssignment(val kind: String) : RefUsage
      data object Bound : RefUsage
      data object Unbound : RefUsage
    }

    fun analyzeReferenceUsage(ref: StarlarkReferenceExpression): RefUsage {
      val name = ref.name ?: return RefUsage.Unbound
      val currentScope = nearestScopeOwner(ref) ?: return RefUsage.Unbound
      val currentScopeData = scopeData(currentScope) ?: return RefUsage.Unbound

      if (isBoundByEnclosingComprehension(ref, name)) return RefUsage.Bound

      val currentFirstOffset = currentScopeData.firstAssignmentByName[name]
      if (currentFirstOffset != null) {
        if (PsiTreeUtil.getParentOfType(ref, StarlarkCompExpression::class.java, false) != null) return RefUsage.Bound
        return if (ref.textOffset < currentFirstOffset) RefUsage.BeforeAssignment(currentScopeData.label) else RefUsage.Bound
      }

      var outer = currentScopeData.parentScope
      while (outer != null) {
        val outerData = scopeData(outer) ?: break
        val outerFirstOffset = outerData.firstAssignmentByName[name]
        if (outerFirstOffset != null) {
          if (outerData.isFunctionScope && isFreeRefBeforeOuterAssignment(currentScope, outerData, outerFirstOffset)) {
            return RefUsage.BeforeAssignment(FREE)
          }
          return RefUsage.Bound
        }
        outer = outerData.parentScope
      }

      return RefUsage.Unbound
    }

    private fun isBoundByEnclosingComprehension(ref: StarlarkReferenceExpression, name: String): Boolean {
      val comp = PsiTreeUtil.getParentOfType(ref, StarlarkCompExpression::class.java, false) ?: return false
      return comp.getCompVariables().any { it.name == name }
    }

    // Handles simple outer->inner calls only. Does not cover deep call chains or indirect calls (e.g., via aliases or containers).
    private fun isFreeRefBeforeOuterAssignment(currentScope: PsiElement, outerData: ScopeData, outerFirstOffset: Int): Boolean {
      val innerFunction = currentScope as? StarlarkFunctionDeclaration ?: return false
      val innerFunctionName = innerFunction.name ?: return false
      val callOffsets = outerData.callOffsetsByFunctionName[innerFunctionName] ?: return false
      return callOffsets.any { it < outerFirstOffset }
    }

    private fun nearestScopeOwner(element: PsiElement): PsiElement? {
      var current: PsiElement? = element
      while (current != null) {
        when (current) {
          is StarlarkFunctionDeclaration, is StarlarkFile -> return current
        }
        current = current.parent
      }
      return null
    }

    private fun scopeData(scope: PsiElement): ScopeData? = scopeDataByScope[scope]

    companion object {
      private const val GLOBAL = "Global"
      private const val LOCAL = "Local"
      private const val FREE = "Free"

      fun getOrBuild(file: StarlarkFile): StarlarkScopeIndex =
        CachedValuesManager.getCachedValue(file) {
          CachedValueProvider.Result.create(build(file), PsiModificationTracker.MODIFICATION_COUNT, file)
        }

      private fun build(file: StarlarkFile): StarlarkScopeIndex {
        data class MutableScopeData(
          val firstAssignmentByName: LinkedHashMap<String, Int>,
          val callOffsetsByFunctionName: LinkedHashMap<String, MutableList<Int>>,
          val parentScope: PsiElement?,
          val kind: ScopeKind,
        )

        val mutable = linkedMapOf<PsiElement, MutableScopeData>()

        fun ensureScope(scope: PsiElement, parentScope: PsiElement?) {
          if (scope !in mutable) {
            mutable[scope] = MutableScopeData(
              firstAssignmentByName = linkedMapOf(),
              callOffsetsByFunctionName = linkedMapOf(),
              parentScope = parentScope,
              kind = when (scope) {
                is StarlarkFile -> ScopeKind.FILE
                is StarlarkFunctionDeclaration -> ScopeKind.FUNCTION
                else -> error("Unexpected scope type: ${scope::class.java.name}")
              },
            )
          }
        }

        fun putFirstBinding(scope: PsiElement?, name: String?, offset: Int) {
          if (name == null) return
          val data = scope?.let(mutable::get) ?: return
          if (name !in data.firstAssignmentByName) data.firstAssignmentByName[name] = offset
        }

        fun putFunctionParameters(function: StarlarkFunctionDeclaration) =
          function.getParameters().forEach { putFirstBinding(function, it.name, it.textOffset) }

        fun loadSymbolLocalName(loadValue: StarlarkLoadValue): String? = when (loadValue) {
          is StarlarkNamedLoadValue -> loadValue.name
          is StarlarkStringLoadValue -> loadValue.getLoadValueExpressionContent()
          else -> null
        }

        fun walk(node: PsiElement, currentScope: PsiElement?) {
          val scopeForNode = when (node) {
            is StarlarkFile, is StarlarkFunctionDeclaration -> node
            else -> currentScope
          }

          if (scopeForNode === node) ensureScope(scopeForNode, currentScope)

          when (node) {
            is StarlarkAssignmentStatement -> putFirstBinding(currentScope, node.name, node.textOffset)

            is StarlarkFunctionDeclaration -> {
              putFirstBinding(currentScope, node.name, node.textOffset)
              putFunctionParameters(node)
            }

            is StarlarkForStatement -> node.getLoopVariables().forEach { putFirstBinding(scopeForNode, it.name, it.textOffset) }

            is StarlarkLoadStatement -> {
              node.getLoadedSymbolsPsi()
                .filterIsInstance<StarlarkLoadValue>()
                .forEach { putFirstBinding(scopeForNode, loadSymbolLocalName(it), it.getLoadValueExpression()?.textOffset ?: it.textOffset) }
            }

            is StarlarkCallExpression -> {
              val calledName = node.getCalledFunctionName()
              val data = scopeForNode?.let(mutable::get)
              if (calledName != null && data != null) {
                data.callOffsetsByFunctionName.getOrPut(calledName) { mutableListOf() } += node.textOffset
              }
            }
          }

          var child = node.firstChild
          while (child != null) {
            walk(child, scopeForNode)
            child = child.nextSibling
          }
        }

        walk(file, null)

        val immutable = mutable.mapValues { (_, data) ->
          ScopeData(
            firstAssignmentByName = data.firstAssignmentByName.toMap(),
            callOffsetsByFunctionName = data.callOffsetsByFunctionName.mapValues { it.value.toList() },
            parentScope = data.parentScope,
            kind = data.kind,
          )
        }

        return StarlarkScopeIndex(immutable)
      }
    }
  }
}
