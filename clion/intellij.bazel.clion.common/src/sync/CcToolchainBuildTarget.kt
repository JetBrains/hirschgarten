package org.jetbrains.bazel.clion.sync

import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.StringsCollection

data class CcToolchainBuildTarget(
  val targetName: String,
  val compilerName: String,
  val cppOption: StringsCollection,
  val cOption: StringsCollection,
  val cCompiler: OutputLocation,
  val cppCompiler: OutputLocation,
  val builtInIncludeDirectories: OutputLocationCollection,
  val sysroot: OutputLocation?,
  val cEnvironment: Map<String, String>,
  val cppEnvironment: Map<String, String>,
  val xcodeInfo: XcodeInfo?,
) : BuildTargetData {

  data class XcodeInfo(
    val macosSdkVersion: String,
    val xcodeVersion: String,
  )
}
