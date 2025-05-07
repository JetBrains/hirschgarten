package org.jetbrains.bazel.cpp.sync.configuration

import org.jetbrains.bazel.cpp.sync.BazelCompilerSettings
import org.jetbrains.bazel.cpp.sync.xcode.XCodeCompilerSettings
import org.jetbrains.bazel.info.BspTargetInfo
import java.io.File

/**
 * Resolve configuration maps, etc. obtained from running the [BazelConfigurationResolver].
 * See com.google.idea.blaze.cpp.BlazeConfigurationResolverResult
 */
data class BazelConfigurationResolverResult(
  val uniqueResolveConfigurations: Map<BazelResolveConfigurationData, BazelResolveConfiguration>,
  val compilerSettings: Map<BspTargetInfo.CToolchainInfo, BazelCompilerSettings>,
  val validHeaderRoots: Set<File>,
  val xcodeProperties: XCodeCompilerSettings?,
) {
  companion object {
    fun empty() =
      BazelConfigurationResolverResult(
        emptyMap(),
        emptyMap(),
        emptySet(),
        null,
      )
  }

  val allConfigurations: List<BazelResolveConfiguration>
    get() = uniqueResolveConfigurations.values.toList()

  val configurationMap: Map<BazelResolveConfigurationData, BazelResolveConfiguration>
    get() = uniqueResolveConfigurations

  fun isValidHeaderRoot(absolutePath: File): Boolean = validHeaderRoots.contains(absolutePath)

  fun isEquivalentConfigurations(other: BazelConfigurationResolverResult): Boolean {
    if (uniqueResolveConfigurations.keys.toSet() != (other.uniqueResolveConfigurations.keys.toSet())) {
      return false
    }
    for (mapEntry in uniqueResolveConfigurations) {
      val config: BazelResolveConfiguration = mapEntry.value
      val otherConfig: BazelResolveConfiguration? = other.uniqueResolveConfigurations[mapEntry.key]
      if (otherConfig == null || !config.isEquivalentConfigurations(otherConfig)) {
        return false
      }
    }
    return validHeaderRoots == other.validHeaderRoots && xcodeProperties == other.xcodeProperties
  }

  internal class Builder {
    var uniqueConfigurations: Map<BazelResolveConfigurationData, BazelResolveConfiguration> = mapOf()
    var compilerSettings: Map<BspTargetInfo.CToolchainInfo, BazelCompilerSettings> = mapOf()
    var validHeaderRoots: Set<File> = setOf()
    var xcodeSettings: XCodeCompilerSettings? = null

    fun build(): BazelConfigurationResolverResult =
      BazelConfigurationResolverResult(
        uniqueConfigurations,
        compilerSettings,
        validHeaderRoots,
        xcodeSettings,
      )
  }
}
