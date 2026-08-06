package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class Position(
  // 0-based
  val line: Int,
  // 0-based
  val character: Int
) {
  companion object {
    val NONE: Position = Position(-1, -1)

    // Converts 1-based numbers to 0-based
    fun fromHumanReadable(line: Int, character: Int): Position =
      Position(line - 1, character - 1)
  }
}
