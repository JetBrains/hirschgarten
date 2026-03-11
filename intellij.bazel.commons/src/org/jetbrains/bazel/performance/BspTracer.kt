package org.jetbrains.bazel.performance

import com.intellij.platform.diagnostic.telemetry.Scope
import com.intellij.platform.diagnostic.telemetry.TelemetryManager
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import org.jetbrains.annotations.ApiStatus

private const val BSP_SCOPE: String = "bsp"

@JvmField
@ApiStatus.Internal
val bspScope = Scope(BSP_SCOPE)

@JvmField
@ApiStatus.Internal
val bspTracer: Tracer = TelemetryManager.getTracer(bspScope)

@JvmField
@ApiStatus.Internal
val bspMeter: Meter = TelemetryManager.getMeter(bspScope)
