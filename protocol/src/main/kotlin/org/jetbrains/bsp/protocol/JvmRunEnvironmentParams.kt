package org.jetbrains.bsp.protocol

data class JvmRunEnvironmentParams(val targets: List<BuildTargetIdentifier>, val originId: String? = null)
