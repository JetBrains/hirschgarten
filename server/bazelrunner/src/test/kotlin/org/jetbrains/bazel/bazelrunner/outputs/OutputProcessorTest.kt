package org.jetbrains.bazel.bazelrunner.outputs

import com.intellij.openapi.util.SystemInfo.isWindows
import com.intellij.util.io.awaitExit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

// Test wrapper to adapt java.lang.Process to SpawnedProcess
class ProcessWrapper(private val process: Process) : SpawnedProcess {
  override val pid: Long = process.pid()
  override val inputStream: InputStream = process.inputStream
  override val errorStream: InputStream = process.errorStream
  override val isAlive: Boolean get() = process.isAlive

  override fun waitFor(timeout: Int, unit: TimeUnit): Boolean = process.waitFor(timeout.toLong(), unit)

  override fun waitFor(): Int = process.waitFor()

  override fun destroy() {
    process.destroyForcibly()
  }

  override suspend fun awaitExit(): Int = process.awaitExit()
}

// Mock ProcessSpawner for testing
class MockProcessSpawner : ProcessSpawner {
  override suspend fun spawnDeferredProcess(
    command: String,
    args: List<String>,
    environment: Map<String, String>,
    redirectErrorStream: Boolean,
    workDirectory: String?,
  ): Deferred<SpawnedProcess> = throw UnsupportedOperationException("Not implemented for tests")

  override fun killProcessTree(process: SpawnedProcess): Boolean {
    if (process is ProcessWrapper) {
      process.destroy()
      return true
    }
    return false
  }

  override fun killProcess(process: SpawnedProcess) {
    if (process is ProcessWrapper) {
      process.destroy()
    }
  }

  override fun killProcess(pid: Int) {
    // Mock implementation - in real tests this would kill the process by PID
    try {
      val process =
        ProcessBuilder(
          if (isWindows) {
            listOf("taskkill", "/F", "/PID", pid.toString())
          } else {
            listOf("kill", "-9", pid.toString())
          },
        ).start()
      process.waitFor()
    } catch (e: Exception) {
      // Ignore errors in mock implementation
    }
  }
}

class OutputProcessorTest {
  @Before
  fun setUp() {
    ProcessSpawner.provideProcessSpawner(MockProcessSpawner())
  }

  @Test
  fun `cancelling waitForExit kills the process`() {
    val process = startHangingProcess()
    val proc = AsyncOutputProcessor(ProcessWrapper(process), OutputCollector())

    process.isAlive shouldBe true
    shouldThrow<CancellationException> {
      runBlocking {
        // Add timeout to prevent test hanging indefinitely
        withTimeout(1000) {
          proc.waitForExit(
            null,
          )
        }
      }
    }
    process.waitFor(1, TimeUnit.SECONDS) shouldBe true
    process.isAlive shouldBe false
  }

  @Test
  fun `cancelling waitForExit kills the server process too`() {
    val process = startHangingProcess()
    val serverProcess = startHangingProcess()
    val serverPidFuture = CompletableFuture.completedFuture(serverProcess.pid())
    val proc = AsyncOutputProcessor(ProcessWrapper(process), OutputCollector())

    process.isAlive shouldBe true
    serverProcess.isAlive shouldBe true
    shouldThrow<CancellationException> {
      runBlocking {
        // Add timeout to prevent test hanging indefinitely
        withTimeout(1000) {
          proc.waitForExit(
            serverPidFuture,
          )
        }
      }
    }
    process.waitFor(1, TimeUnit.SECONDS) shouldBe true
    process.isAlive shouldBe false
    serverProcess.waitFor(1, TimeUnit.SECONDS) shouldBe true
    serverProcess.isAlive shouldBe false
  }

  @Test
  fun `successful waitForExit returns exit code and does not kill the server`() {
    val process = startQuickProcess()
    val serverProcess = startHangingProcess()
    val serverPidFuture = CompletableFuture.completedFuture(serverProcess.pid())
    val proc = AsyncOutputProcessor(ProcessWrapper(process), OutputCollector())

    serverProcess.isAlive shouldBe true
    val exitCode =
      runBlocking {
        // Add timeout to prevent test hanging indefinitely
        withTimeout(1000) {
          proc.waitForExit(
            serverPidFuture,
          )
        }
      }
    exitCode shouldBe 0
    process.isAlive shouldBe false
    serverProcess.isAlive shouldBe true
    serverProcess.destroyForcibly()
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

  fun startQuickProcess(): Process = startProcess(listOf("cmd", "/c", "echo hello"), listOf("sh", "-c", "echo hello"))

  fun startHangingProcess(): Process =
    startProcess(
      listOf("cmd", "/c", "timeout /t 9999999 >nul"),
      listOf("sh", "-c", "sleep 9999999"),
    )
}
