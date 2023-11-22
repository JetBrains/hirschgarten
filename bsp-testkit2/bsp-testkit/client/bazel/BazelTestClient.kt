package org.jetbrains.bsp.testkit.client.bazel

import ch.epfl.scala.bsp4j.InitializeBuildParams
import org.jetbrains.bsp.testkit.client.TestClient
import java.nio.file.Path

class BazelTestClient(
  override val workspacePath: Path,
  override val initializeParams: InitializeBuildParams,
  private val bazelCache: Path,
  private val bazelOutputBase: Path
) : TestClient(
  workspacePath,
  initializeParams,
  { s: String ->
    s.replace("\$WORKSPACE", workspacePath.toString())
      .replace("\$BAZEL_CACHE", bazelCache.toString())
      .replace("\$BAZEL_OUTPUT_BASE_PATH", bazelOutputBase.toString())
      .replace("\$OS", osFamily)
  }
) {
  companion object {
    private val osFamily: String = System.getProperty("os.name").toLowerCase().let { osName ->
      when {
        osName.startsWith("windows") -> "win"
        osName.startsWith("mac") -> "macos"
        else -> "linux"
      }
    }
  }
}