package org.jetbrains.bazel.target.sync

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspace.TESTLIB_SUFFIXES
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

internal class TargetUtilsSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val bspTargets = environment.workspace.targets
    val customFileToTarget = calculateFileToTarget(bspTargets)
    val project = environment.project
    val libraries = environment.workspace.libraries
    environment.deferredApplyActions += {
      project.targetUtils.saveTargets(
        targets = bspTargets,
        fileToTarget = customFileToTarget,
        libraryItems = libraries,
      )
    }
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
        for (suffix in TESTLIB_SUFFIXES) {
          val testlibLabel = try {
            Label.parse("${target.id}$suffix")
          } catch (_: Exception) {
            null
          }
          if (testlibLabel != null && labelToTarget.containsKey(testlibLabel)) {
            testlibToOwner[testlibLabel] = target.id
            break
          }
        }
      }
    }
    return testlibToOwner
  }
}
