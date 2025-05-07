package org.jetbrains.bazel.cpp.sync.configuration

import org.jetbrains.bazel.commons.ExecutionRootPath
import org.jetbrains.bazel.cpp.sync.BazelCompilerSettings
import org.jetbrains.bazel.info.BspTargetInfo

/** Data for clustering {@link BazelResolveConfiguration} by "equivalence".
 * See com.google.idea.blaze.cpp.BlazeResolveConfigurationData
 * */
data class BazelResolveConfigurationData(
  val compilerSettings: BazelCompilerSettings,
  val toolchainIdeInfo: BspTargetInfo.CToolchainInfo,
  // Everything from CIdeInfo except for sources, headers, etc.
  // That is parts that influence the flags, but not the actual input files.
  val localCopts: List<String>,
  // From the cpp compilation context provider.
  // These should all be for the entire transitive closure.
  val transitiveIncludeDirectories: List<ExecutionRootPath>,
  val transitiveQuoteIncludeDirectories: List<ExecutionRootPath>,
  val transitiveDefines: List<String>,
  val transitiveSystemIncludeDirectories: List<ExecutionRootPath>,
) {
  constructor(
    compilerSettings: BazelCompilerSettings,
    cIdeInfo: BspTargetInfo.CppTargetInfo,
    toolchainIdeInfo: BspTargetInfo.CToolchainInfo,
  ) : this(
    compilerSettings = compilerSettings,
    toolchainIdeInfo = toolchainIdeInfo,
    localCopts = cIdeInfo.coptsList,
    transitiveIncludeDirectories = cIdeInfo.transitiveIncludeDirectoryList.map { ExecutionRootPath(it) },
    transitiveQuoteIncludeDirectories = cIdeInfo.transitiveQuoteIncludeDirectoryList.map { ExecutionRootPath(it) },
    transitiveDefines = cIdeInfo.transitiveDefineList,
    transitiveSystemIncludeDirectories = cIdeInfo.transitiveSystemIncludeDirectoryList.map { ExecutionRootPath(it) },
  )
}
