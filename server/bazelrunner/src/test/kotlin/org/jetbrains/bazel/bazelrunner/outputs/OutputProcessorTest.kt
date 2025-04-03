package org.jetbrains.bazel.bazelrunner.outputs

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bsp.testkit.client.MockClient
import org.junit.Test
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.cancellation.CancellationException

class OutputProcessorTest {
  @Test
  fun `AsyncOutputProcessor waitForExit`() {
    val process = startHangingProcess()
    val proc = AsyncOutputProcessor(process, OutputCollector())
    val pidFuture = CompletableFuture.completedFuture(process.pid())

    process.isAlive shouldBe true
    try {
      runBlocking {
        val job =
          async {
            proc.waitForExit(pidFuture, BspClientLogger(MockClient()))
          }

        delay(600) // wait for one iteration
        job.cancel("Test cancellation") // trigger cancellation path

        try {
          job.join()
        } catch (e: CancellationException) {
          // Expected behavior - cancellation was propagated
        }

        // Wait for process to terminate
        var timeout = 0
        while (!process.waitFor(100, java.util.concurrent.TimeUnit.MILLISECONDS) && timeout < 20) {
          yield()
          timeout++
        }

        process.isAlive shouldBe false
      }
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
          listOf("cmd", "/c", "ping -t localhost")
        else ->
          listOf("sh", "-c", "tail -f /dev/null")
      }

    val process =
      ProcessBuilder(command)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

    return process!!
  }
}
