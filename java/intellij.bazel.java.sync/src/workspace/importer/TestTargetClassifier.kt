package org.jetbrains.bazel.workspace.importer

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.RawBuildTarget

@ApiStatus.Internal
object TestTargetClassifier {

  fun calculateTargetsToMarkAsTest(targets: List<RawBuildTarget>): Set<Label> {
    val allDependentsAreTests = mutableMapOf<Label, Boolean>()
    val hasSourcelessRunnersDependant = mutableSetOf<Label>()
    for (dependent in targets) {
      val dependentIsTest = dependent.isTestTarget()
      val dependentIsSourcelessTestRunner = dependent.kind.ruleType == RuleType.TEST && dependent.sources.isEmpty()
      for ((targetKey) in dependent.dependencies) {
        val dependency = targetKey.label
        if (dependency == dependent.id) continue
        allDependentsAreTests.merge(dependency, dependentIsTest, Boolean::and)
        if (dependentIsSourcelessTestRunner && dependent.id.packagePath == dependency.packagePath) {
          hasSourcelessRunnersDependant += dependency
        }
      }
    }
    fun RawBuildTarget.isTestLibrary(): Boolean {
      return kind.ruleType == RuleType.LIBRARY && allDependentsAreTests[id] == true && id in hasSourcelessRunnersDependant
    }
    return targets.mapNotNullTo(mutableSetOf()) {
      if (it.isTestTarget() || it.isTestLibrary()) it.id else null
    }
  }

  private fun RawBuildTarget.isTestTarget(): Boolean = isTestOnly || kind.ruleType == RuleType.TEST
}
