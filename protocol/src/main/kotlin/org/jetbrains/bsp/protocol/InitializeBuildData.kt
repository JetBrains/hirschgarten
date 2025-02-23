package org.jetbrains.bsp.protocol

data class InitializeBuildData(val clientClassesRootDir: String? = null, val openTelemetryEndpoint: String? = null)
