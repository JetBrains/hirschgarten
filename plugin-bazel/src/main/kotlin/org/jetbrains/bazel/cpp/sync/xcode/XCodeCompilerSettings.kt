package org.jetbrains.bazel.cpp.sync.xcode

import java.nio.file.Path

class XCodeCompilerSettings(val developerDir: Path, val sdkRootPath: Path) {
  fun asEnvironmentVariables(): Map<String, String> =
    mapOf(
      "DEVELOPER_DIR" to developerDir.toString(),
      "SDKROOT" to sdkRootPath.toString(),
    )
}
