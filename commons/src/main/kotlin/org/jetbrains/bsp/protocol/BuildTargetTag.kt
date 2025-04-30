package org.jetbrains.bsp.protocol

object BuildTargetTag {
  const val MANUAL = "manual"
  const val NO_IDE = "no-ide"
  const val LIBRARIES_OVER_MODULES = "libraries-over-modules" // used with `experimental_prioritize_libraries_over_modules_target_kinds`
}
