package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

internal data class DependencySourcesItem(val target: Label, val sources: List<Path>)
