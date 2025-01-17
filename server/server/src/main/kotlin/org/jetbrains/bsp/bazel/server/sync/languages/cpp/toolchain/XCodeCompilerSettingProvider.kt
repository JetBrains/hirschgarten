package org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain

import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import java.nio.file.Path

/**
 * Interface for fetching Xcode information from the system.
 * This has to be an interface and accompanying global service to allow mocking in tests.
 */
interface XCodeCompilerSettingProvider {
  fun fromContext(bazelRunner: BazelRunner): XCodeCompilerSettings?
}

class XCodeCompilerSettingProviderImpl : XCodeCompilerSettingProvider {
  override fun fromContext(bazelRunner: BazelRunner): XCodeCompilerSettings? {
    // todo: implement
    return null
  }
}

class XCodeCompilerSettings(val developerDir: Path, val sdkRootPath: Path) {
  // If the user is using xcode, then the clang compiler found by bazel under the given compiler path
  // is just an entrypoint of compiler. It is xcode-select tool that can control which compiler and libraries
  // this entry point should actually use.
  // DEVELOPER_DIR and SDKROOT are the environment variable that can control this behavior.
  // DEVELOPER_DIR can tell xcode-select to use the toolchain of a specific vesion. SDKROOT will control which libraries
  // and header the compiler should use.
  // So when the user is using xcode, these two variable should be also passed to Clion together
  // (by setting them in the compiler wrapper) together with the clang's location to ensure that CLion is using exactly the
  // same compiler version and same libraries with bazel
  fun asEnvironmentVariables(): Map<String, String> =
    mapOf(
      "DEVELOPER_DIR" to developerDir.toString(),
      "SDKROOT" to sdkRootPath.toString(),
    )
}
