package org.jetbrains.bsp.protocol

data class WorkspaceBuildTargetsResult(val targets: List<RawBuildTarget>, val hasError: Boolean = false)
