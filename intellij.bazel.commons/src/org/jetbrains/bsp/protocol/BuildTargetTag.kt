package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object BuildTargetTag {
  const val MANUAL = "manual"
  const val NO_IDE = "no-ide"
}
