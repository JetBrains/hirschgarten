package org.jetbrains.bazel.run.synthetic

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget

@ApiStatus.Internal
object SyntheticRunTargetUtils {
  private val escapePattern = Regex("[^A-Za-z0-9_]")

  fun getSyntheticTargetLabel(vararg packageParts: String, targetName: String): Label {
    return ResolvedLabel(
      repo = Main,
      packagePath = Package(listOf(Constants.DOT_BAZELBSP_DIR_NAME, Constants.SYNTHETIC_TARGETS_DIR_NAME) + packageParts),
      target = SingleTarget(targetName),
    )
  }

  fun escapeTargetLabel(input: String): String {
    return input.replace(escapePattern, "_")
  }
}
