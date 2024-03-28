package org.jetbrains.bsp.testkit.client.bazel

import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.InitializeBuildParams
import org.jetbrains.bsp.testkit.client.BasicTestClient
import java.nio.file.Path
import java.util.Locale

class BazelTestClient<Server : BuildServer, Client : BuildClient>(
  workspacePath: Path,
  initializeParams: InitializeBuildParams,
  private val bazelCache: Path,
  private val bazelOutputBase: Path,
  client: Client,
  serverClass: Class<Server>
) : BasicTestClient<Server, Client>(
  workspacePath,
  initializeParams,
  { s: String ->
    s.replace("\$WORKSPACE", workspacePath.toString())
      .replace("\$BAZEL_CACHE", bazelCache.toString())
      .replace("\$BAZEL_OUTPUT_BASE_PATH", bazelOutputBase.toString())
      .replace("\$OS", osFamily)
  },
  client = client,
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
