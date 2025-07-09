package org.jetbrains.bazel.performance

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import org.jetbrains.bazel.performance.telemetry.TelemetryManager

const val BSP_SCOPE = "bsp"

val telemetryManager: TelemetryManager by lazy { TelemetryManager.getInstance() }

val bspTracer: Tracer by lazy { telemetryManager.getTracer(BSP_SCOPE) }

val bspMeter: Meter by lazy { telemetryManager.getMeter(BSP_SCOPE) }
