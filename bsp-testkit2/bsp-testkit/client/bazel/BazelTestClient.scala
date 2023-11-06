package org.jetbrains.bsp.testkit.client.bazel

import ch.epfl.scala.bsp4j.InitializeBuildParams
import org.jetbrains.bsp.testkit.client.TestClient

import java.nio.file.Path

object BazelTestClient {
  private val osFamily: String = {
    val osName = System.getProperty("os.name").toLowerCase()
    if (osName.startsWith("windows")) {
      "win"
    } else if (osName.startsWith("mac")) {
      "macos"
    } else {
      "linux"
    }
  }
}

class BazelTestClient(override val workspacePath: Path, override val initializeParams: InitializeBuildParams,
                      val bazelCache: Path, val bazelOutputBase: Path)
  extends TestClient(workspacePath, initializeParams, (s: String) =>
    s.replace("$WORKSPACE", workspacePath.toString)
      .replace("$BAZEL_CACHE", bazelCache.toString)
      .replace("$BAZEL_OUTPUT_BASE_PATH", bazelOutputBase.toString)
      .replace("$OS", BazelTestClient.osFamily)
  )
