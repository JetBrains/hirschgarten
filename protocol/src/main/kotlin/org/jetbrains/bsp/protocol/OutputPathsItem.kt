package org.jetbrains.bsp.protocol

data class OutputPathsItem(val target: BuildTargetIdentifier, val outputPaths: List<OutputPathItem>)
