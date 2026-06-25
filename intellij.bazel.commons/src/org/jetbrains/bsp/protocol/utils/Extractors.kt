package org.jetbrains.bsp.protocol.utils

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget

@ApiStatus.Internal
inline fun <reified Data> BuildTarget.extractData(): Data? = this.data.filterIsInstance<Data>().singleOrNull()

@ApiStatus.Internal
fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? = target.extractData()

@ApiStatus.Internal
fun extractScalaBuildTarget(target: BuildTarget): ScalaBuildTarget? = target.extractData()

@ApiStatus.Internal
fun extractKotlinBuildTarget(target: BuildTarget): KotlinBuildTarget? = target.extractData()

@ApiStatus.Internal
fun extractProtobufBuildTarget(target: BuildTarget): ProtobufBuildTarget? = target.extractData()

@ApiStatus.Internal
fun extractJvmBuildTarget(target: BuildTarget): JvmBuildTarget? = target.extractData()
