package org.jetbrains.bazel.performance

import com.intellij.platform.diagnostic.telemetry.Scope
import com.intellij.platform.diagnostic.telemetry.TelemetryManager
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer

const val BSP_SCOPE: String = "bsp"

private val bspScope = Scope(BSP_SCOPE)

@JvmField
val bspTracer: Tracer = TelemetryManager.getTracer(bspScope)

@JvmField
val bspMeter: Meter = TelemetryManager.getMeter(bspScope)
