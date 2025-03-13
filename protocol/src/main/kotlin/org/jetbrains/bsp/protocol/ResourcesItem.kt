package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class ResourcesItem(val target: Label, val resources: List<String>)
