package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class CleanCacheParams(val targets: List<Label>)
