package org.jetbrains.plugins.bsp.performance.testing

import com.intellij.platform.diagnostic.telemetry.Scope
import com.intellij.platform.diagnostic.telemetry.TelemetryManager

@JvmField
internal val bspScope = Scope("bsp")

@JvmField
internal val bspTracer = TelemetryManager.getTracer(bspScope)

@JvmField
internal val bspMeter = TelemetryManager.getMeter(bspScope)
