package org.jetbrains.bsp.protocol

data class JvmTestEnvironmentParams(val targets: List<BuildTargetIdentifier>, val originId: String? = null)
