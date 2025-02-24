package org.jetbrains.bsp.protocol

import org.jetbrains.bsp.protocol.BuildTargetIdentifier

data class WorkspaceBuildTargetsPartialParams(val targets: List<BuildTargetIdentifier>)
