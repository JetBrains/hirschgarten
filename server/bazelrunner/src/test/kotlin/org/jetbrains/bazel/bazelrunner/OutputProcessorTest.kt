package org.jetbrains.bazel.bazelrunner

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.bazelrunner.outputs.AsyncOutputProcessor
import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bsp.testkit.client.MockClient
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class OutputProcessorTest {
  @Test
  fun `AsyncOutputProcessor waitForExit`() {
    val process = startHangingProcess()
    val proc = AsyncOutputProcessor(startHangingProcess(), OutputCollector())
    val pidFuture = CompletableFuture.completedFuture(process.pid())

    runBlocking {
      proc.waitForExit(
        pidFuture,
        BspClientLogger(MockClient()),
      )
    }
    1 shouldBe 1
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
    return null!!
  }
}
