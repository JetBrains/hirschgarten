package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildTargetIdentifier

data class WorkspaceBuildTargetsPartialParams(val targets: List<BuildTargetIdentifier>)
