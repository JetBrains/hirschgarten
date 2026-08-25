package org.jetbrains.bazel.clion.sync

import org.jetbrains.bsp.protocol.BuildTargetData

data class CcToolchainBuildTarget(
  val targetName: String,
  val compilerName: String,
  val cppOption: List<String>,
  val cOption: List<String>,
  val cCompiler: ExecutionRootPath,
  val cppCompiler: ExecutionRootPath,
  val builtInIncludeDirectories: List<ExecutionRootPath>,
  val sysroot: ExecutionRootPath,
  val cEnvironment: Map<String, String>,
  val cppEnvironment: Map<String, String>,
) : BuildTargetData
