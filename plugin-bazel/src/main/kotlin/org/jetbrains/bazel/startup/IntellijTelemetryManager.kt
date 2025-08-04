package org.jetbrains.bazel.startup

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import org.jetbrains.bazel.performance.BSP_SCOPE
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import com.intellij.platform.diagnostic.telemetry.Scope as IntellijScope
import com.intellij.platform.diagnostic.telemetry.TelemetryManager as IntellijTelemetryManager

object IntellijTelemetryManager : TelemetryManager {
  private val platformTelemetryManager by lazy { IntellijTelemetryManager.getInstance() }

  override fun getTracer(): Tracer = platformTelemetryManager.getTracer(IntellijScope(BSP_SCOPE))

  override fun getMeter(): Meter = platformTelemetryManager.getMeter(IntellijScope(BSP_SCOPE))
}
