package org.jetbrains.bsp.protocol

data class ScalaMainClassesParams(val targets: List<BuildTargetIdentifier>, val originId: String? = null)
