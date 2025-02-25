package org.jetbrains.bsp.protocol

data class DidChangeBuildTarget(val changes: List<BuildTargetEvent>)
