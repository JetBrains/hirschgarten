package org.jetbrains.bsp.protocol

data class PythonOptionsItem(val target: BuildTargetIdentifier, val interpreterOptions: List<String>)
