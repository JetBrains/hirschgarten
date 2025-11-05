package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.Language
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bsp.protocol.BuildTarget

object SyntheticRunTargetUtils {
  private val escapePattern = Regex("[^A-Za-z0-9_]")

  fun getSyntheticTargetLabel(vararg packageParts: String, targetName: String): Label {
    return ResolvedLabel(
      repo = Main,
      packagePath = Package(listOf(".bazelbsp", "synthetic_targets") + packageParts),
      target = SingleTarget(targetName),
    )
  }

  fun getTemplateGenerators(target: BuildTarget, language: Language): List<SyntheticRunTargetTemplateGenerator> =
    SyntheticRunTargetTemplateGenerator.ep.allForLanguage(language)
      .filter { it.isSupported(target) }

  fun escapeTargetLabel(input: String): String {
    return input.replace(escapePattern, "_")
  }
}
