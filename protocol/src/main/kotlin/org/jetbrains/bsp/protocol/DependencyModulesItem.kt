package org.jetbrains.bsp.protocol

data class DependencyModulesItem(val target: BuildTargetIdentifier, val modules: List<DependencyModule>)
