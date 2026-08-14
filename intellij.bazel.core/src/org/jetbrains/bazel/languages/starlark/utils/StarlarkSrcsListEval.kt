package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.references.findBuildFilePathFor
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.languages.starlark.repomapping.findContainingBazelRepo
import java.util.EnumSet

/**
 * Heuristically evaluates "srcs = ..." and "resources == ..." expressions and find targets which may
 * include the given source file as a source
 */
@ApiStatus.Internal
class StarlarkSrcsListEval(private val project: Project) {

  private val fileTargetsCache = HashMap<VirtualFile, List<StarlarkTargetInfo>>()

  @RequiresReadLock
  fun findTargetsForSourceFile(file: VirtualFile): Map<Label, EnumSet<Kind>> {
    val root = findContainingBazelRepo(file) ?: return emptyMap()
    val buildFile = findBuildFilePathFor(file, root) ?: return emptyMap()

    val relativePath = VfsUtil.getRelativePath(file, buildFile.parent) ?: return emptyMap()

    val targets = fileTargetsCache.computeIfAbsent(buildFile) { calculateTargets(it) }
    return targets.mapNotNull { target ->
      val set: EnumSet<Kind> = EnumSet.noneOf<Kind>(Kind::class.java)
      if (target.srcsExpr?.let { srcsExpressionMatchPath(it, relativePath) } == true) set.add(Kind.Srcs)
      if (target.resourcesExpr?.let { srcsExpressionMatchPath(it, relativePath) } == true) set.add(Kind.Resources)
      if (set.isEmpty())
        return@mapNotNull null
      target.label to set
    }.toMap()
  }

  private fun calculateTargets(buildFile: VirtualFile): List<StarlarkTargetInfo> {
    val starlarkFile = PsiManager.getInstance(project).findFile(buildFile) as? StarlarkFile
                       ?: return emptyList()

    return starlarkFile.getTargetRules().mapNotNull { targetRuleExpr: StarlarkCallExpression ->
      val ruleName = targetRuleExpr.getCalledFunctionName()
      val targetName = targetRuleExpr.getNameAttributeValue()
      val targetLabel: ResolvedLabel = calculateLabel(project, buildFile, targetName)
                                       ?: return@mapNotNull null

      val srcsExpression: PsiElement? = targetRuleExpr.getArgumentList()?.getSrcsArgument()?.getValue()
      val resourcesExpression: PsiElement? = targetRuleExpr.getArgumentList()?.getResourcesArgument()?.getValue()

      StarlarkTargetInfo(kind = ruleName, label = targetLabel, srcsExpr = srcsExpression, resourcesExpr = resourcesExpression)
    }
  }

  private class StarlarkTargetInfo(
    val kind: String?,
    val label: Label,
    val srcsExpr: PsiElement?,
    val resourcesExpr: PsiElement?,
  )

  enum class Kind {
    Srcs, Resources
  }
}
