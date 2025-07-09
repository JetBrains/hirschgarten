package org.jetbrains.bazel.startup

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import com.intellij.platform.diagnostic.telemetry.Scope as IntellijScope
import com.intellij.platform.diagnostic.telemetry.TelemetryManager as IntellijTelemetryManager

object IntellijTelemetryManager : TelemetryManager {
  private val platformTelemetryManager by lazy { IntellijTelemetryManager.getInstance() }

  override fun getTracer(scope: String): Tracer = platformTelemetryManager.getTracer(IntellijScope(scope))

  override fun getMeter(scope: String): Meter = platformTelemetryManager.getMeter(IntellijScope(scope))
}
