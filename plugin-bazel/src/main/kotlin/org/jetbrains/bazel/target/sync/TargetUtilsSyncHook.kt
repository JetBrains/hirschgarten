package org.jetbrains.bazel.target.sync

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.target.sync.projectStructure.targetUtilsDiff
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

internal class TargetUtilsSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val bspTargets =
      environment.resolver
        .getOrFetchResolvedWorkspace(taskId = environment.taskId)
        .targets
    val targetUtilsDiff = environment.diff.targetUtilsDiff
    targetUtilsDiff.bspTargets = bspTargets
    targetUtilsDiff.fileToTarget = calculateFileToTarget(bspTargets)
  }

  private fun calculateFileToTarget(targets: List<RawBuildTarget>): Map<Path, List<Label>> {
    val resultMap = HashMap<Path, MutableList<Label>>()
    val labelToTarget = targets.associateBy { it.id }
    val testlibToOwner = buildTestlibToOwnerMap(targets, labelToTarget)

    for (target in targets) {
      // Map sources to owner test target for testlibs, or to the target itself otherwise
      val targetLabel = testlibToOwner[target.id] ?: target.id
      for (source in target.sources) {
        val path = source.path
        resultMap.computeIfAbsent(path) { ArrayList() }.add(targetLabel)
      }
    }
    return resultMap
  }

  private fun buildTestlibToOwnerMap(targets: List<BuildTarget>, labelToTarget: Map<Label, BuildTarget>): Map<Label, Label> {
    val testlibToOwner = HashMap<Label, Label>()
    for (target in targets) {
      target as RawBuildTarget
      if (target.kind.ruleType == org.jetbrains.bazel.commons.RuleType.TEST && target.sources.isEmpty()) {
        val testlibLabel = try {
          Label.parse("${target.id}.testlib")
        } catch (_: Exception) {
          null
        }
        if (testlibLabel != null && labelToTarget.containsKey(testlibLabel)) {
          testlibToOwner[testlibLabel] = target.id
        }
      }
    }
    return testlibToOwner
  }
}
