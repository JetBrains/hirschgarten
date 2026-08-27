package org.jetbrains.bazel.clion.sync

import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection

data class CcToolchainBuildTarget(
  val targetName: String,
  val compilerName: String,
  val cppOption: List<String>,
  val cOption: List<String>,
  val cCompiler: OutputLocation,
  val cppCompiler: OutputLocation,
  val builtInIncludeDirectories: OutputLocationCollection,
  val sysroot: OutputLocation?,
  val cEnvironment: Map<String, String>,
  val cppEnvironment: Map<String, String>,
) : BuildTargetData
