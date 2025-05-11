package org.jetbrains.bazel.performance

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import org.jetbrains.bazel.performance.telemetry.Scope
import org.jetbrains.bazel.performance.telemetry.TelemetryManager

@JvmField
internal val bspScope = Scope("bsp")

val telemetryManager: TelemetryManager by lazy { TelemetryManager.getInstance() }

val bspTracer: Tracer by lazy { telemetryManager.getTracer(bspScope) }

val bspMeter: Meter by lazy { telemetryManager.getMeter(bspScope) }
