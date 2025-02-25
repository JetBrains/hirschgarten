package org.jetbrains.bsp.protocol.utils

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.PythonBuildTarget
import ch.epfl.scala.bsp4j.ScalaBuildTarget
import org.jetbrains.bsp.protocol.AndroidBuildTarget
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget

public fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? = target.data as? PythonBuildTarget

public fun extractScalaBuildTarget(target: BuildTarget): ScalaBuildTarget? = target.data as? ScalaBuildTarget

public fun extractAndroidBuildTarget(target: BuildTarget): AndroidBuildTarget? = target.data as? AndroidBuildTarget

public fun extractGoBuildTarget(target: BuildTarget): GoBuildTarget? = target.data as? GoBuildTarget

public fun extractKotlinBuildTarget(target: BuildTarget): KotlinBuildTarget? = target.data as? KotlinBuildTarget

public fun extractJvmBuildTarget(target: BuildTarget): JvmBuildTarget? =
  target.data as? JvmBuildTarget
    ?: extractAndroidBuildTarget(target)?.jvmBuildTarget
    ?: extractKotlinBuildTarget(target)?.jvmBuildTarget
    ?: extractScalaBuildTarget(target)?.jvmBuildTarget
