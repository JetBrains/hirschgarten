package org.jetbrains.bazel.bazelrunner.outputs

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

// Simple OS detection for tests
private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

class OutputProcessorTest {
  @Test
  fun `cancelling waitForExit kills the process`() {
    val process = startHangingProcess()
    val proc = AsyncOutputProcessor(process, OutputCollector())

    process.isAlive shouldBe true
    shouldThrow<CancellationException> {
      runBlocking {
        // Add timeout to prevent test hanging indefinitely
        withTimeout(1000) {
          proc.waitForExit()
        }
      }
    }
    process.waitFor(1, TimeUnit.SECONDS) shouldBe true
    process.isAlive shouldBe false
  }

  @Test
  fun `successful waitForExit returns exit code and does not kill the server`() {
    val process = startQuickProcess()
    val proc = AsyncOutputProcessor(process, OutputCollector())

    val exitCode =
      runBlocking {
        // Add timeout to prevent test hanging indefinitely
        withTimeout(1000) {
          proc.waitForExit()
        }
      }
    exitCode shouldBe 0
    process.isAlive shouldBe false
  }

  fun startProcess(windowsCommand: List<String>, unixCommand: List<String>): Process {
    val command =
      when {
        isWindows ->
          windowsCommand
        else ->
          unixCommand
      }

    return ProcessBuilder(command)
      .redirectOutput(ProcessBuilder.Redirect.INHERIT)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
      .start()
  }

  fun startQuickProcess(): Process =
    startProcess(
      listOf("cmd", "/c", "echo hello"),
      listOf("sh", "-c", "echo hello")
    )

  fun startHangingProcess(): Process =
    startProcess(
      listOf("cmd", "/c", "timeout /t 9999999 >nul"),
      listOf("sh", "-c", "sleep 9999999"),
    )
}
