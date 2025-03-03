package org.jetbrains.bsp.protocol

data class JvmMainClass(val className: String, val arguments: List<String>)
