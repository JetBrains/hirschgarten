package org.jetbrains.bsp.protocol

data class BuildClientCapabilities(val languageIds: List<String>, val jvmCompileClasspathReceiver: Boolean = true)
