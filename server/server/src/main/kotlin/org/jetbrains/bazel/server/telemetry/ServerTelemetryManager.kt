package org.jetbrains.bazel.server.telemetry

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import org.jetbrains.bazel.performance.BSP_SCOPE
import org.jetbrains.bazel.performance.telemetry.TelemetryManager

/**
 * Server-side implementation of TelemetryManager that provides basic OpenTelemetry components
 */
object ServerTelemetryManager : TelemetryManager {
  override fun getTracer(): Tracer = GlobalOpenTelemetry.getTracer(BSP_SCOPE)

  override fun getMeter(): Meter = GlobalOpenTelemetry.getMeter(BSP_SCOPE)
}
