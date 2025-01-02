package org.jetbrains.bsp.testkit.client.bazel

import java.nio.file.Path
import java.util.Locale

class BazelJsonTransformer(
  private val workspacePath: Path,
  private val bazelCache: Path,
  private val bazelOutputBase: Path,
) {
  fun transformJson(s: String): String {
    return s
      .replace("\$WORKSPACE", workspacePath.toString())
      .replace("\$BAZEL_CACHE", bazelCache.toString())
      .replace("\$BAZEL_OUTPUT_BASE_PATH", bazelOutputBase.toString())
      .replace("\$OS", osFamily)
  }

  companion object {
    private val osFamily: String = System.getProperty("os.name").lowercase(Locale.getDefault()).let { osName ->
      when {
        osName.startsWith("windows") -> "win"
        osName.startsWith("mac") -> "macos"
        else -> "linux"
      }
    }
  }
}
