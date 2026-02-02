package org.jetbrains.bazel.bazelrunner.outputs

import com.intellij.execution.process.OSProcessUtil
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class OutputProcessor(private val process: Process, handler: OutputHandler? = null) {
  val stdoutCollector = OutputCollector()
  val stderrCollector = OutputCollector()

  private val executorService = Executors.newCachedThreadPool()
  private val runningProcessors = mutableListOf<Future<*>>()

  init {
    start(CollectableInputStream(process.inputStream, stdoutCollector), handler)
    start(CollectableInputStream(process.errorStream, stderrCollector), handler)
  }

  private fun shutdown() {
    // Process is terminated here and stdout/stderr are closed already, Read buffered leftovers
    runningProcessors.forEach {
      it.get(1, TimeUnit.MINUTES) // Output handles should not be _that_ heavy after process is terminated
    }
    executorService.shutdown()
  }

  private fun start(inputStream: InputStream, handler: OutputHandler?) {
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
              handler?.onNextLine(line)
            }
          }
        } catch (e: Throwable) {
          if (Thread.currentThread().isInterrupted) return@Runnable
          logger.error("Error processing output", e)
        }
      }

    executorService.submit(runnable).also { runningProcessors.add(it) }
  }

  suspend fun waitForExit(killProcessTreeOnCancel: Boolean): Int =
    coroutineScope {
      try {
        return@coroutineScope process.awaitExit()
      } catch (e: CancellationException) {
        try {
          if (killProcessTreeOnCancel) {
            OSProcessUtil.killProcessTree(process)
          } else {
            OSProcessUtil.killProcess(process)
          }
        } catch (_: Throwable) {
        }
        throw e
      } finally {
        shutdown()
      }
    }

  private class CollectableInputStream(val delegate: InputStream,
                                       val collector: OutputCollector): InputStream() {

    override fun close() {
      delegate.close()
    }

    override fun read(): Int {
      return delegate.read()
        .also { if (it != -1) collector.append(it) }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
      return delegate.read(b, off, len).also {
        if (it > 0) collector.append(b, off, it)
      }
    }

    override fun available(): Int {
      return delegate.available()
    }
  }


  companion object {
    private val logger = LoggerFactory.getLogger(OutputProcessor::class.java)
  }
}
