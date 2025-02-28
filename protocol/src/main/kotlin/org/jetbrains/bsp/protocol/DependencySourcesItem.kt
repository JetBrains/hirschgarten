package org.jetbrains.bsp.protocol

data class DependencySourcesItem(val target: BuildTargetIdentifier, val sources: List<String>)
