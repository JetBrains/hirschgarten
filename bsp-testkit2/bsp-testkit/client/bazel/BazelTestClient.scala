package org.jetbrains.bsp.testkit.client.bazel

import ch.epfl.scala.bsp4j.InitializeBuildParams
import org.jetbrains.bsp.testkit.client.TestClient

import java.nio.file.Path
import java.util.stream.Collectors
import scala.util.Try

object BazelTestClient {

  import scala.sys.process._

  private def processOutput(workspace: Path, command: Seq[String]): String = {
    Process(command, Some(workspace.toFile)).!!
  }

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

  def transform(workspacePath: Path): String => String = {
    Try(processOutput(workspacePath, Seq("bazel", "version"))).toOption match {
      case Some(version) => println(s"Testkit client uses Bazel version:\n$version")
      case None => throw new IllegalStateException("Bazel is not installed")
    }

    val bazelCachePath = processOutput(workspacePath, Seq("bazel", "info", "execution_root"))
      .lines()
      .collect(Collectors.joining())

    val bazelOutputBasePath = processOutput(workspacePath, Seq("bazel", "info", "output_base"))
      .lines()
      .collect(Collectors.joining())

    (s: String) =>
      s.replace("$WORKSPACE", workspacePath.toString)
        .replace("$BAZEL_CACHE", bazelCachePath)
        .replace("$BAZEL_OUTPUT_BASE_PATH", bazelOutputBasePath)
        .replace("$OS", osFamily)
  }
}

class BazelTestClient(override val workspacePath: Path, override val initializeParams: InitializeBuildParams) extends TestClient(workspacePath, initializeParams, BazelTestClient.transform(workspacePath))
