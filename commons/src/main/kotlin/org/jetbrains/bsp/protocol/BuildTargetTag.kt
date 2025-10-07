package org.jetbrains.bsp.protocol

object BuildTargetTag {
  const val MANUAL = "manual"
  const val NO_IDE = "no-ide"

  /**
   * Targets marked with this tag are included in the sync process but are hidden in the target tree.
   */
  const val HIDDEN = "hidden"
}
