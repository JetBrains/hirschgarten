package org.jetbrains.bsp.protocol

import org.jetbrains.bsp.protocol.BuildTarget

data class NonModuleTargetsResult(val nonModuleTargets: List<BuildTarget>)
