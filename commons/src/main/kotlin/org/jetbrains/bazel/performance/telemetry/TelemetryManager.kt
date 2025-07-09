package org.jetbrains.bazel.performance.telemetry

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer

interface TelemetryManager {
  fun getTracer(scope: Scope): Tracer

  fun getMeter(scope: Scope): Meter

  companion object {
    private lateinit var instance: TelemetryManager

    fun getInstance(): TelemetryManager = instance

    fun provideTelemetryManager(instance: TelemetryManager) {
      this.instance = instance
    }
  }
}
