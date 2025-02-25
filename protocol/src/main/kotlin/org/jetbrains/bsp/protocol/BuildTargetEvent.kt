package org.jetbrains.bsp.protocol

data class BuildTargetEvent(val target: BuildTargetIdentifier, val kind: BuildTargetEventKind? = null)
