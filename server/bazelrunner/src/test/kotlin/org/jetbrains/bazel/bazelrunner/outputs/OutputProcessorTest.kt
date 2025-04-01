package org.jetbrains.bazel.bazelrunner.outputs

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bsp.testkit.client.MockClient
import org.junit.Test
import java.util.concurrent.CompletableFuture

class OutputProcessorTest {
  @Test
  fun `AsyncOutputProcessor waitForExit`() {
    val process = startHangingProcess()
    val proc = AsyncOutputProcessor(process, OutputCollector()) // Use the same process
    val pidFuture = CompletableFuture.completedFuture(process.pid())

    process.isAlive shouldBe true
    try {
      runBlocking {
        withTimeout(5000) { // Add timeout to prevent test hanging indefinitely
          proc.waitForExit(
            pidFuture,
            BspClientLogger(MockClient()),
          )
        }
      }
      process.isAlive shouldBe false
    } finally {
      // Ensure process is terminated even if test fails
      if (process.isAlive) {
        process.destroyForcibly()
      }
    }
  }

  fun startHangingProcess(): Process {
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    val command =
      when {
        isWindows ->
          listOf("cmd", "/c", "timeout /t 9999999 >nul")
        else ->
          listOf("sh", "-c", "sleep infinity")
      }

    val process =
      ProcessBuilder(command)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

    return process!!
  }
}
