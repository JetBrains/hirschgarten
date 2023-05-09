package org.jetbrains.magicmetamodel.impl

import com.intellij.openapi.diagnostic.logger
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

public object PerformanceLogger {
  private val log = logger<PerformanceLogger>()

  @OptIn(ExperimentalTime::class)
  public fun <T> logPerformance(computationId: String, block: () -> T): T {
    log.debug("Task '${computationId}' started")
    return measureTimedValue(block).let {
      log.debug("Task '$computationId' finished in ${it.duration.inWholeMilliseconds}ms")
      it.value
    }
  }

  @OptIn(ExperimentalTime::class)
  public suspend fun <T> logPerformanceSuspend(computationId: String, block: suspend () -> T): T {
    log.debug("Task '${computationId}' started")
    return measureTimedValue { block() }.let {
      log.debug("Task '$computationId' finished in ${it.duration.inWholeMilliseconds}ms")
      it.value
    }
  }
}