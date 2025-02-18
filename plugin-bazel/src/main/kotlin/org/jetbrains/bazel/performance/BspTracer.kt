package org.jetbrains.bazel.performance

import com.intellij.platform.diagnostic.telemetry.Scope
import com.intellij.platform.diagnostic.telemetry.TelemetryManager

@JvmField
internal val bspScope = Scope("bsp")

@JvmField
val bspTracer = TelemetryManager.getTracer(bspScope)

@JvmField
internal val bspMeter = TelemetryManager.getMeter(bspScope)
