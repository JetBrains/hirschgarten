package org.jetbrains.bsp.sdkcompat.telemetry

import com.intellij.platform.diagnostic.telemetry.OtlpConfiguration

object Endpoint {
  fun getTraceEndpoint(): String? = OtlpConfiguration.getTraceEndpoint()
}
