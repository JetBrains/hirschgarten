package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

data class DependencySourcesItem(val target: CanonicalLabel, val sources: List<Path>)
