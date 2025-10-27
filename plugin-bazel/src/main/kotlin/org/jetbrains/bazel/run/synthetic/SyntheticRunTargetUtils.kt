package org.jetbrains.bazel.run.synthetic

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget

object SyntheticRunTargetUtils {
  private val escapePattern = Regex("[^A-Za-z0-9_]")

  fun getSyntheticTargetLabel(vararg packageParts: String, targetName: String): Label {
    return ResolvedLabel(
      repo = Main,
      packagePath = Package(listOf(".bazelbsp/synthetic_targets") + packageParts),
      target = SingleTarget(targetName)
    )
  }
  
  fun escapeTargetLabel(input: String): String  {
    return input.replace(escapePattern, "_")
  } 
}
