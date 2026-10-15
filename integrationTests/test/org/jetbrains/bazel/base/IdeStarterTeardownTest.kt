package org.jetbrains.bazel.base

import kotlinx.coroutines.runBlocking
import org.jetbrains.intellij.build.dependencies.JdkDownloader.OS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class IdeStarterTeardownTest {
  @TempDir
  lateinit var tempDir: Path

  private var foreignServer: Process? = null
  private var ownedServer: Process? = null

  @AfterEach
  fun stopForeignServer() {
    listOfNotNull(foreignServer, ownedServer).forEach { server ->
      server.descendants().forEach { it.destroyForcibly() }
      server.destroyForcibly()
    }
  }

  @Test
  fun `teardown leaves the Bazel server of another run alone`() {
    startForeignServer()

    PerMethodTest().tearDown()

    assertTrue(checkNotNull(foreignServer).isAlive, "The teardown stopped the Bazel server of another run.")
  }

  @Test
  fun `teardown of a PER_CLASS test leaves the Bazel server alone while the IDE is open`() {
    startForeignServer()

    PerClassTest().tearDown()

    assertTrue(checkNotNull(foreignServer).isAlive, "The teardown stopped a Bazel server between two methods of a PER_CLASS test.")
  }

  @Test
  fun `teardown finds only the Bazel servers of the owned output bases`() {
    startForeignServer()
    val ownedOutputBase = tempDir.resolve("owned-output-base")
    ownedServer = startServer(ownedOutputBase)

    val found = runBlocking { IdeStarterBaseProjectTest.findBazelServers(setOf(HermeticityGuard.normalize(ownedOutputBase.toString()))) }

    assertEquals(listOf(checkNotNull(ownedServer).pid()), found.map { it.pid })
  }

  /** A process with the command line of a Bazel server from the `ide-tests` dir of another run. */
  private fun startForeignServer() {
    foreignServer = startServer(tempDir.resolve("other-run-output-base"))
  }

  private fun startServer(outputBase: Path): Process {
    assumeTrue(OS.current != OS.WINDOWS)
    return ProcessBuilder(
      "sh", "-c", "sleep 600; :", "sh",
      "A-server.jar",
      "--output_base=$outputBase",
      "--workspace_directory=${tempDir.resolve("run/ide-tests/cache/projects/unpacked/simpleJavaTest")}",
    ).start()
  }

  private class PerMethodTest : IdeStarterBaseProjectTest()

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  private class PerClassTest : IdeStarterBaseProjectTest()
}
