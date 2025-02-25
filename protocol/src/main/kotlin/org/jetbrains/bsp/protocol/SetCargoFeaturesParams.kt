package org.jetbrains.bsp.protocol

data class SetCargoFeaturesParams(val packageId: String, val features: Set<String>)
