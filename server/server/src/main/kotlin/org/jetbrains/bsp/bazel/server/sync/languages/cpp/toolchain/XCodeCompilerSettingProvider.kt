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
  fun asEnvironmentVariables(): Map<String, String> =
    mapOf(
      "DEVELOPER_DIR" to developerDir.toString(),
      "SDKROOT" to sdkRootPath.toString(),
    )
}
