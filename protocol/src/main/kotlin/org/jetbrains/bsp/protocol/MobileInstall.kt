package org.jetbrains.bsp.protocol

/**
 * See [mobile-install docs](https://bazel.build/docs/user-manual#start)
 */
public enum class MobileInstallStartType(public val value: Int) {
  NO(1),
  COLD(2),
  WARM(3),
  DEBUG(4),
}

public data class MobileInstallParams(
  val target: BuildTargetIdentifier,
  val originId: String,
  val targetDeviceSerialNumber: String,
  val startType: MobileInstallStartType,
  val adbPath: String?,
)

public data class MobileInstallResult(val statusCode: StatusCode, var originId: String? = null)
