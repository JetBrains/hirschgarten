package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class BspJvmClasspath(val runtimeClasspath: List<Path>, val compileClasspath: List<Path>)
