package org.jetbrains.bazel.bazelrunner.outputs

import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BazelFeatureFlags
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.coroutines.cancellation.CancellationException

abstract class OutputProcessor(private val process: SpawnedProcess, vararg loggers: OutputHandler) {
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
            // Don't use readLine() because it doesn't preserve line separators
            val currentLine = StringBuilder()
            var lookAheadChar: Int? = null
            while (!Thread.currentThread().isInterrupted) {
              var nextChar: Int
              if (lookAheadChar != null) {
                nextChar = lookAheadChar
                lookAheadChar = null
              }
              else {
                nextChar = reader.read()
              }
              if (nextChar == -1) {
                if (currentLine.isEmpty()) break
              }
              else {
                currentLine.append(nextChar.toChar())
              }
              // Don't split on \r if it's actually \r\n
              if (nextChar.toChar() == '\r') {
                // Only read the next char if it's already available, because PTY terminal can end a line with just \r to overwrite it.
                // This .ready() check is probably not 100% reliable, worst case scenario we could send \n as a separate line.
                if (reader.ready()) {
                  nextChar = reader.read()
                  if (nextChar == '\n'.code) {
                    currentLine.append(nextChar.toChar())
                  }
                  else {
                    lookAheadChar = nextChar
                  }
                }
              }
              val line: String
              if (currentLine.isNotEmpty() && (currentLine.last() == '\n' || currentLine.last() == '\r' || nextChar == -1)) {
                line = currentLine.toString()
                currentLine.clear()
              }
              else {
                continue
              }
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
        val processSpawner = ProcessSpawner.getInstance()
        if (BazelFeatureFlags.killClientTreeOnCancel) {
          processSpawner.killProcessTree(process)
        }
        processSpawner.killProcess(process)

        if (BazelFeatureFlags.killServerOnCancel && serverPidFuture?.isDone == true) {
          val pid = serverPidFuture.get()
          processSpawner.killProcess(pid.toInt())
        }
        throw e
      } finally {
        shutdown()
      }
    }
}
