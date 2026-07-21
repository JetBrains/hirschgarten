package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object BuildTargetTag {
  const val MANUAL: String = "manual"
  const val NO_IDE: String = "no-ide"
  const val MAVEN_COORDINATES: String = "maven_coordinates"
}
