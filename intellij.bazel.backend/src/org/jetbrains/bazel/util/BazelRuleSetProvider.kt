package org.jetbrains.bazel.util

import com.intellij.aspect.lib.Rules
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelRelease

@ApiStatus.Internal
data class BazelRuleSet(
  val aspectLanguage: Rules,
  val rulesetNames: List<String>,
  /**
   * Whether Bazel exposes this language natively before the Bazel 8 external rules migration.
   *
   * For Bazel 8 and later, bundled languages are only enabled when this language has explicit
   * autoload hints and Bazel reports matching external autoloads, which preserves the external-rules
   * path for languages such as protobuf.
   */
  val isBundled: Boolean,
  val autoloadHints: List<String> = emptyList(),
  val hostLocations: List<String> = emptyList(),
  val deprecated: String? = null,
)

@ApiStatus.Internal
fun BazelRuleSet.isBundledFor(bazelRelease: BazelRelease, externalAutoloads: List<String>): Boolean {
  if (!isBundled) return false
  if (bazelRelease.major < 8) return true
  // Bazel 8+ only restores the bundled path for languages that opt in via [autoloadHints].
  // Languages without [autoloadHints] (e.g. Protobuf) must instead be discovered as an
  // external ruleset by name. Match against both [rulesetNames] and [autoloadHints]
  // because --incompatible_autoload_externally accepts ruleset names (e.g. "rules_java")
  // as well as individual symbol names (e.g. "JavaInfo", "PyInfo").
  if (autoloadHints.isEmpty()) return false
  return (rulesetNames + autoloadHints).any { it in externalAutoloads }
}

@ApiStatus.Internal
interface BazelRuleSetProvider {
  fun ruleSets() : List<BazelRuleSet>

  companion object {
    val ep: ExtensionPointName<BazelRuleSetProvider> =
      ExtensionPointName.create<BazelRuleSetProvider>("org.jetbrains.bazel.bazelRuleSetProvider")

    fun all(): List<BazelRuleSet> {
      val result = mutableListOf<BazelRuleSet>()
      ep.forEachExtensionSafe { provider ->
        result.addAll(provider.ruleSets())
      }
      return result
    }
  }
}
