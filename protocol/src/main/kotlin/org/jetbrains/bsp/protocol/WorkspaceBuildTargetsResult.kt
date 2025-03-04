package org.jetbrains.bsp.protocol

data class WorkspaceBuildTargetsResult(val targets: List<BuildTarget>, val hasError: Boolean = false)
