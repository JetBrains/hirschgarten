package org.jetbrains.bsp.protocol

data class InitializeBuildParams(val clientClassesRootDir: String? = null, val featureFlags: FeatureFlags = FeatureFlags())
