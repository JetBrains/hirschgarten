package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class SourcesItem(val target: Label, val sources: List<SourceItem>)
