package org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain

import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import java.nio.file.Path

class BazelCppToolchainResolver(
  val bazelInfo: BazelInfo,
  val bazelRunner: BazelRunner,
  val compilerVersionChecker: CompilerVersionChecker,
  val xCodeProvider: XCodeCompilerSettingProvider
) {

  constructor(bazelInfo: BazelInfo, bazelRunner: BazelRunner) : this(
    bazelInfo,
    bazelRunner,
    CompilerVersionCheckerImpl(),
    XCodeCompilerSettingProviderImpl(),
  )

  val xCodeSetting = xCodeProvider.fromContext(bazelRunner)
  fun getCompilerVersion( cppExecutable: String, cExecutable: String): String {
    val cppCompilerVersion = resolveCompilerVersion(bazelInfo.execRoot, cppExecutable, xCodeSetting)
    val cCompilerVersion = resolveCompilerVersion(bazelInfo.execRoot, cExecutable, xCodeSetting)
    return mergeCompilerVersions(cCompilerVersion, cppCompilerVersion) ?: ""
  }


  private fun mergeCompilerVersions(cCompilerVersion: String?, cppCompilerVersion: String?): String? {
    return cppCompilerVersion ?: cCompilerVersion
  }

  private fun resolveCompilerVersion(execRoot: String, executable: String, xcode: XCodeCompilerSettings?): String? {
    if (executable.isBlank()) return null
    return compilerVersionChecker.getCompilerVersion(execRoot, Path.of(executable), xcode)
  }

}
