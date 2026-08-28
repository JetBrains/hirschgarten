package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import org.jetbrains.bazel.clion.BazelClionBundle
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bsp.protocol.BuildTarget

context(ctx: CcImportContext)
internal fun buildToolchainMap(): Map<WorkspaceTargetKey, WorkspaceTargetKey> {
  val toolchains = ctx.snapshot.targets.allTargets()
    .filter { it.hasBuildData<CcToolchainBuildTarget>() }
    .map { it.key }
    .toSet()

  // pick an arbitrary fallback toolchain
  val fallbackToolchain = toolchains.firstOrNull()

  // early return if there are no toolchains
  if (fallbackToolchain == null) return emptyMap()

  val problems = mutableMapOf<WorkspaceTargetKey, List<WorkspaceTargetKey>>()
  val result = mutableMapOf<WorkspaceTargetKey, WorkspaceTargetKey>()

  for (target in ctx.snapshot.targets.allTargets()) {
    if (!target.hasBuildData<CcBuildTarget>()) continue
    val candidates = findTargetToolchain(target, toolchains)

    if (candidates.size != 1) {
      problems[target.key] = candidates
    }

    result[target.key] = candidates.firstOrNull() ?: fallbackToolchain
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
      BazelClionBundle.message("cc.toolchain.no.dependencies.found")
    }
    else {
      problem.value.joinToString(", ") { it.presentable() }
    }

    "${problem.key.presentable()}: $candidates"
  }

  ctx.reportEvent(
    message = BazelClionBundle.message("cc.toolchain.unexpected.dependency.count", problems.size),
    description = description,
    severity = MessageEvent.Kind.WARNING,
  )
}
