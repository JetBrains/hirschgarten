package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

@ApiStatus.Internal
data class InverseSourcesResult(val targets: Map<Path, List<Label>>)
