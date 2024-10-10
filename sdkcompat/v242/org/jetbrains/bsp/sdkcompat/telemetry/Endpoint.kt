package org.jetbrains.bsp.sdkcompat.telemetry

import com.intellij.platform.diagnostic.telemetry.impl.getOtlpEndPoint

object Endpoint {
  fun getTraceEndpoint(): String? = getOtlpEndPoint()
}
