package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

data class InverseSourcesResult(val targets: Map<Path, List<Label>>)
