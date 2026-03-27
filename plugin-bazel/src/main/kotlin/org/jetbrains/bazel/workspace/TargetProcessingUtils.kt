package org.jetbrains.bazel.workspace

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget

/**
 * Suffixes used in Bazel target names to denote test library targets.
 * Targets with these suffixes are typically stripped to their base name for IDE purposes.
 */
val TESTLIB_SUFFIXES = listOf(".testlib", "-test-lib")

/**
 * Checks if a target name contains any of the test library suffixes.
 */
fun String.containsTestlibSuffix(): Boolean = TESTLIB_SUFFIXES.any { this.endsWith(it) }

/**
 * Strips all test library suffixes from a target name.
 */
fun String.stripTestlibSuffix(): String {
  var result = this
  for (suffix in TESTLIB_SUFFIXES) {
    result = result.removeSuffix(suffix)
  }
  return result
}

/**
 * Result of processing targets for .testlib stripping and filtering.
 *
 * @property allProcessedTargets All targets including both original .testlib targets and their stripped versions
 * @property strippedLabels Set of labels that are stripped versions (without .testlib)
 * @property targetsForMapping Filtered targets to be stored in target utils mapping (excludes original .testlib labels)
 */
data class ProcessedTargetsResult(
  val allProcessedTargets: List<Label>,
  val strippedLabels: Set<Label>,
  val targetsForMapping: List<Label>
)

/**
 * Processes a list of targets to handle .testlib suffix stripping:
 * - For targets containing .testlib in their name, creates stripped versions without .testlib
 * - Keeps both original .testlib targets and stripped versions for module creation
 * - Returns filtered list for target utils mapping (excluding original .testlib targets)
 *
 * This ensures that:
 * 1. Original .testlib targets get file sources added
 * 2. Stripped targets are fetched/cached but don't get file sources
 * 3. Only stripped targets (not original .testlib) are stored in target utils mapping
 */
fun processTargetsForTestlibStripping(targets: List<Label>): ProcessedTargetsResult {

  val strippedLabels = mutableSetOf<Label>()
  val originalLabelsWithStrippedVersion = mutableSetOf<Label>()

  val allProcessedTargets = targets.flatMap { label ->
    val targetName = label.targetName
    if (targetName.containsTestlibSuffix()) {
      // Strip test library suffixes and create a new label with the stripped name
      val strippedTargetName = targetName.stripTestlibSuffix()
      val strippedLabel = when (label) {
        is ResolvedLabel -> {
          ResolvedLabel(
            label.repo,
            label.packagePath,
            SingleTarget(strippedTargetName)
          )
        }
        else -> null
      }

      if (strippedLabel != null) {
        strippedLabels.add(strippedLabel)
        originalLabelsWithStrippedVersion.add(label)
        listOf(label, strippedLabel)
      } else {
        listOf(label)
      }
    } else {
      listOf(label)
    }
  }

  // Filter targets for mapping - exclude original .testlib labels
  val targetsForMapping = allProcessedTargets.filterNot { originalLabelsWithStrippedVersion.contains(it) }
  return ProcessedTargetsResult(
    allProcessedTargets = allProcessedTargets,
    strippedLabels = strippedLabels,
    targetsForMapping = targetsForMapping
  )
}
