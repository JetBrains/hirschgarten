package org.jetbrains.bsp.protocol.utils

import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget

private inline fun <reified Data> extractData(target: BuildTarget): Data? = target.data as? Data

fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? = extractData(target)

fun extractScalaBuildTarget(target: BuildTarget): ScalaBuildTarget? = extractData(target)

fun extractGoBuildTarget(target: BuildTarget): GoBuildTarget? = extractData(target)

fun extractKotlinBuildTarget(target: BuildTarget): KotlinBuildTarget? = extractData(target)

fun extractJvmBuildTarget(target: BuildTarget): JvmBuildTarget? =
  extractData(target)
    ?: extractKotlinBuildTarget(target)?.jvmBuildTarget
    ?: extractScalaBuildTarget(target)?.jvmBuildTarget
