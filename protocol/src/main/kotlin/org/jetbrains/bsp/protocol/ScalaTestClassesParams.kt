package org.jetbrains.bsp.protocol

data class ScalaTestClassesParams(val targets: List<BuildTargetIdentifier>, val originId: String? = null)
