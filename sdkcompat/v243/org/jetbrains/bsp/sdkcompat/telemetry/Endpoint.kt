package org.jetbrains.bsp.sdkcompat.telemetry

import com.intellij.platform.diagnostic.telemetry.OtlpConfiguration

// v243: impl.getOtlpEndPoint renamed to OtlpConfiguration.getTraceEndpoint()
object Endpoint {
  fun getTraceEndpoint(): String? = OtlpConfiguration.getTraceEndpoint()
}
