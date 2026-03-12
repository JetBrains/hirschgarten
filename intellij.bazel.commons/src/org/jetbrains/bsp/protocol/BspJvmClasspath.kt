package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
data class BspJvmClasspath(val runtimeClasspath: List<Path>, val compileClasspath: List<Path>)
