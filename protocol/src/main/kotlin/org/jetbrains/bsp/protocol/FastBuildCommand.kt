package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class FastBuildCommand(val builderScript: String, val builderArgs: List<String>, val outputFile: Path)
