package org.jetbrains.bsp.protocol

data class JvmCompileClasspathItem(val target: BuildTargetIdentifier, val classpath: List<String>)
