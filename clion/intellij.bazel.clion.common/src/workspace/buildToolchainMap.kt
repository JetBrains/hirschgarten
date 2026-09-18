package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.BazelCLionCommonBundle
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
context(ctx: CcImportContext)
fun buildToolchainMap(): Map<WorkspaceTargetKey, WorkspaceTargetKey> {
  val toolchains = ctx.snapshot.targets.allTargets()
    .filter { it.hasBuildData<CcToolchainBuildTarget>() }
    .map { it.key }
    .toSet()

  // pick an arbitrary fallback toolchain
  val fallbackToolchain = toolchains.firstOrNull()

  val problems = mutableMapOf<WorkspaceTargetKey, List<WorkspaceTargetKey>>()
  val result = mutableMapOf<WorkspaceTargetKey, WorkspaceTargetKey>()

  for (target in ctx.snapshot.targets.allTargets()) {
    if (!target.hasBuildData<CcBuildTarget>()) continue
    val candidates = findTargetToolchain(target, toolchains)

    // if the CcInfo was provided by an aspect, we do not know what toolchain the aspect used
    val plainTarget = target.key.aspectIds.ids.isEmpty()

    // no need to report an error if the target has no sources
    val hasSources = !target.sources.isEmpty() || !target.generatedSources.isEmpty()

    if (hasSources && (candidates.size > 1 || (candidates.isEmpty() && plainTarget))) {
      problems[target.key] = candidates
    }

    val toolchain = candidates.firstOrNull() ?: fallbackToolchain
    if (toolchain != null) result[target.key] = toolchain
  }

  reportProblems(problems)

  return result
}

private fun findTargetToolchain(target: BuildTarget, toolchains: Set<WorkspaceTargetKey>): List<WorkspaceTargetKey> {
  val candidates = target.dependencies
    .filter { it.kind == DependencyLabelKind.TOOLCHAIN }
    .map { it.targetKey }
    .filter { toolchains.contains(it) }

  // prefer the dependencies of type toolchain if any were found
  if (candidates.isNotEmpty()) return candidates

  // for older Bazel versions (<= 7) toolchain dependencies are just regular dependencies
  return target.dependencies.map { it.targetKey }.filter { toolchains.contains(it) }
}

context(ctx: CcImportContext)
private fun reportProblems(problems: Map<WorkspaceTargetKey, List<WorkspaceTargetKey>>) {
  if (problems.isEmpty()) return

  val description = problems.entries.joinToString("\n") { problem ->
    val candidates = if (problem.value.isEmpty()) {
      BazelCLionCommonBundle.message("cc.toolchain.no.dependencies.found")
    }
    else {
      problem.value.joinToString(", ") { it.presentable() }
    }

    "${problem.key.presentable()}: $candidates"
  }

  ctx.reportEvent(
    message = BazelCLionCommonBundle.message("cc.toolchain.unexpected.dependency.count", problems.size),
    description = description,
    severity = MessageEvent.Kind.WARNING,
  )
}
