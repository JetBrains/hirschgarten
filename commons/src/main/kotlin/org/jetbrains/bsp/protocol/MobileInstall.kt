package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

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
  val target: Label,
  val originId: String,
  val targetDeviceSerialNumber: String,
  val startType: MobileInstallStartType,
  val adbPath: Path?,
)

public data class MobileInstallResult(val statusCode: StatusCode, var originId: String? = null)
