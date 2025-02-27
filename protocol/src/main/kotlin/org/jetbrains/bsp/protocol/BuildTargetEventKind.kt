package org.jetbrains.bsp.protocol

enum class BuildTargetEventKind(val value: Int) {
  Created(1),
  Changed(2),
  Deleted(3),
}
