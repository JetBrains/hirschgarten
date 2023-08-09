package org.jetbrains.magicmetamodel.impl

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.magicmetamodel.impl.BenchmarkFlags.isBenchmark
import org.jetbrains.magicmetamodel.impl.BenchmarkFlags.metricsFile
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.writeText
import kotlin.time.measureTimedValue

public object PerformanceLogger {
  private val log = logger<PerformanceLogger>()

  private val metrics: java.util.HashMap<String, Long> = HashMap()

  public fun <T> logPerformance(computationId: String, block: () -> T): T {
    log.info("Task '${computationId}' started")
    return measureTimedValue(block).let {
      log.info("Task '$computationId' finished in ${it.duration.inWholeMilliseconds}ms")
      val clearKey = computationId.replace("""[\s-_]+""".toRegex(), ".").lowercase(Locale.getDefault())
      logMemory(clearKey)
      logMilliseconds(clearKey, it.duration.inWholeMilliseconds)
      it.value
    }
  }

  private fun logMilliseconds(clearKey: String, value: Long) {
    logMetric("$clearKey.ms", value)
  }

  public suspend fun <T> logPerformanceSuspend(computationId: String, block: suspend () -> T): T {
    log.info("Task '${computationId}' started")
    return measureTimedValue { block() }.let {
      log.info("Task '$computationId' finished in ${it.duration.inWholeMilliseconds}ms")
      it.value
    }
  }

  private fun logMemory(key: String) {
    if (isBenchmark()) {
      System.gc()
      val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
      logMetric("$key.mb", usedMemory)
    }
  }

  private fun logMetric(key: String, usedMemory: Long) {
    if(metricsFile() != null) {
      metrics[key] = usedMemory
    }
  }

  public fun dumpMetrics() {
    metricsFile()?.let { dumpMetrics(it) }
  }

  public fun dumpMetrics(file: Path) {
    val metricsText = metrics.map {
      "${it.key} ${it.value}"
    }.joinToString("\n")
    file.writeText(metricsText)
  }
}
