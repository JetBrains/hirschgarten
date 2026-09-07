package org.jetbrains.bazel.clion.workspace

import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

data class CcCompilerInfo(
  val cCompiler: Path,
  val cppCompiler: Path,
  val cSwitches: List<String>,
  val cppSwitches: List<String>,
  val name: String,
  val environment: Map<String, String>,
  val builtinIncludes: OutputLocationCollection,
  val sysroot: OutputLocation?,
)

context(ctx: CcImportContext)
internal fun buildCompilerSettings(): Map<WorkspaceTargetKey, CcCompilerInfo> {
  val result = mutableMapOf<WorkspaceTargetKey, CcCompilerInfo>()

  for (target in ctx.snapshot.targets.allTargets()) {
    val toolchainInfo = target.extractData<CcToolchainBuildTarget>() ?: continue

    val compilerInfo = CcCompilerInfo(
      // TODO: do we want to print a warning if the path is invalid?
      cCompiler = ctx.resolve(toolchainInfo.cCompiler) ?: continue,
      cppCompiler = ctx.resolve(toolchainInfo.cppCompiler) ?: continue,
      cSwitches = toolchainInfo.cOption,
      cppSwitches = toolchainInfo.cppOption,
      name = toolchainInfo.compilerName,
      // TODO: port environment processing i.e. com.google.idea.blaze.cpp.environment.EnvironmentProcessor
      environment = mergeEnvironments(toolchainInfo.cEnvironment, toolchainInfo.cppEnvironment),
      builtinIncludes = toolchainInfo.builtInIncludeDirectories,
      sysroot = toolchainInfo.sysroot,
    )

    result[target.key] = compilerInfo
  }

  return result
}

private fun mergeEnvironments(vararg environments: Map<String, String>): Map<String, String> {
  val merged = mutableMapOf<String, String>()
  for (environment in environments) {
    merged.putAll(environment)
  }

  return merged
}
