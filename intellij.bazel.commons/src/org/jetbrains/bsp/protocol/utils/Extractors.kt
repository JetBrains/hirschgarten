package org.jetbrains.bsp.protocol.utils

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget

private inline fun <reified Data> extractData(target: BuildTarget): Data? = target.data as? Data

@ApiStatus.Internal
fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? = extractData(target)

@ApiStatus.Internal
fun extractScalaBuildTarget(target: BuildTarget): ScalaBuildTarget? = extractData(target)

@ApiStatus.Internal
fun extractGoBuildTarget(target: BuildTarget): GoBuildTarget? = extractData(target)

@ApiStatus.Internal
fun extractKotlinBuildTarget(target: BuildTarget): KotlinBuildTarget? = extractData(target)

@ApiStatus.Internal
fun extractProtobufBuildTarget(target: BuildTarget): ProtobufBuildTarget? = extractData(target)

@ApiStatus.Internal
fun extractJvmBuildTarget(target: BuildTarget): JvmBuildTarget? =
  extractData(target)
    ?: extractKotlinBuildTarget(target)?.jvmBuildTarget
    ?: extractScalaBuildTarget(target)?.jvmBuildTarget
    ?: extractProtobufBuildTarget(target)?.jvmBuildTarget
