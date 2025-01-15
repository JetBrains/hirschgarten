package org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain

import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import java.nio.file.Path

class BazelCppToolchainResolver(
  val bazelInfo: BazelInfo,
  val bazelRunner: BazelRunner,
  val compilerVersionChecker: CompilerVersionChecker = CompilerVersionCheckerImpl(),
  val xCodeProvider: XCodeCompilerSettingProvider = XCodeCompilerSettingProviderImpl(),
) {
  val xCodeSetting = xCodeProvider.fromContext(bazelRunner)

  fun getCompilerVersion(cppExecutable: Path, cExecutable: Path): String? {
    val cppCompilerVersion = resolveCompilerVersion(Path.of(bazelInfo.execRoot), cppExecutable, xCodeSetting)
    if (cppCompilerVersion != null) return cppCompilerVersion
    return resolveCompilerVersion(Path.of(bazelInfo.execRoot), cExecutable, xCodeSetting)
  }

  private fun resolveCompilerVersion(
    execRoot: Path,
    executable: Path,
    xcode: XCodeCompilerSettings?,
  ): String? = compilerVersionChecker.getCompilerVersion(execRoot, executable, xcode)
}
