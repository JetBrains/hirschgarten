package org.jetbrains.bsp.protocol

data class ResourcesItem(val target: BuildTargetIdentifier, val resources: List<String>)
