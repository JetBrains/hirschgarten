package org.jetbrains.bazel.bazelrunner.outputs

import com.intellij.execution.process.OSProcessUtil
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.coroutineScope
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.coroutines.cancellation.CancellationException

abstract class OutputProcessor(private val process: Process, vararg loggers: OutputHandler) {
  val stdoutCollector = OutputCollector()
  val stderrCollector = OutputCollector()

  private val executorService = Executors.newCachedThreadPool()
  protected val runningProcessors = mutableListOf<Future<*>>()

  init {
    start(process.inputStream, stdoutCollector, *loggers)
    start(process.errorStream, stderrCollector, *loggers)
  }

  protected open fun shutdown() {
    executorService.shutdown()
  }

  protected abstract fun isRunning(): Boolean

  protected fun start(inputStream: InputStream, vararg handlers: OutputHandler) {
    val runnable =
      Runnable {
        try {
          BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var prevLine: String? = null

            while (!Thread.currentThread().isInterrupted) {
              val line = reader.readLine() ?: return@Runnable
              if (line == prevLine) continue
              prevLine = line
              if (isRunning()) {
                handlers.forEach { it.onNextLine(line) }
              } else {
                break
              }
            }
          }
        } catch (e: IOException) {
          if (Thread.currentThread().isInterrupted) return@Runnable
          throw RuntimeException(e)
        }
      }

    executorService.submit(runnable).also { runningProcessors.add(it) }
  }

  suspend fun waitForExit(serverPidFuture: CompletableFuture<Long>?): Int =
    coroutineScope {
      try {
        return@coroutineScope process.awaitExit()
      } catch (e: CancellationException) {
        OSProcessUtil.killProcessTree(process)
        OSProcessUtil.killProcess(process)
        if (serverPidFuture?.isDone == true) {
          val pid = serverPidFuture.get()
          OSProcessUtil.killProcess(pid.toInt())
        }
        throw e
      } finally {
        shutdown()
      }
    }
}
