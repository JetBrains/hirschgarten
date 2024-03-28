package org.jetbrains.bsp.testkit.client.bazel

import ch.epfl.scala.bsp4j.InitializeBuildParams
import org.jetbrains.bsp.testkit.client.MockServer
import org.jetbrains.bsp.testkit.client.TestClient
import java.nio.file.Path
import java.util.Locale

class BazelTestClient<Server: MockServer>(
  workspacePath: Path,
  initializeParams: InitializeBuildParams,
  private val bazelCache: Path,
  private val bazelOutputBase: Path,
  serverClass: Class<Server>
) : TestClient<Server>(
  workspacePath,
  initializeParams,
  { s: String ->
    s.replace("\$WORKSPACE", workspacePath.toString())
      .replace("\$BAZEL_CACHE", bazelCache.toString())
      .replace("\$BAZEL_OUTPUT_BASE_PATH", bazelOutputBase.toString())
      .replace("\$OS", osFamily)
  },
  serverClass = serverClass
) {
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
