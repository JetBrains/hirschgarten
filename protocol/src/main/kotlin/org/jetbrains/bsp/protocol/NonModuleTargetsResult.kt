package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildTarget

data class NonModuleTargetsResult(val nonModuleTargets: List<BuildTarget>)
