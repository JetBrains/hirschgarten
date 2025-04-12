package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

data class FastBuildParams(val label: Label, val file: Path, val tempDir: Path)
