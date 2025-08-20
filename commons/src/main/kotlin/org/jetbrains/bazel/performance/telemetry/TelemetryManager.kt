package org.jetbrains.bazel.performance.telemetry

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer

interface TelemetryManager {
  fun getTracer(): Tracer

  fun getMeter(): Meter

  companion object {
    private lateinit var instance: TelemetryManager

    fun getInstance(): TelemetryManager {
      if (!::instance.isInitialized) {
        throw IllegalStateException("TelemetryManager has not been initialized. Call provideTelemetryManager() first.")
      }
      return instance
    }

    fun provideTelemetryManager(instance: TelemetryManager) {
      this.instance = instance
    }
  }
}
