package org.jetbrains.bazel.clion.sync

import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocationCollection

data class CcBuildTarget(val ruleContext: RuleContext?, val compilationContext: CompilationContext) : BuildTargetData {

  /**
   * Information collected from rule attributes directly; may not be present
   * for custom rules, since the semantic of their rule attributes is unknown.
   */
  data class RuleContext(
    val headers: OutputLocationCollection,
    val textualHeaders: OutputLocationCollection,
    val copts: List<String>,
    val conlyopts: List<String>,
    val cxxopts: List<String>,
    val args: List<String>,
    val includePrefix: String,
    val stripIncludePrefix: String,
  )

  /**
   * Information collected from the CcInfoProvider; should always be present.
   */
  data class CompilationContext(
    val headers: OutputLocationCollection,
    val defines: List<String>,
    val includes: OutputLocationCollection,
    val quoteIncludes: OutputLocationCollection,
    val systemIncludes: OutputLocationCollection,
  )
}
