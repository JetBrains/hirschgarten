package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

public data class JvmBinaryJarsParams(val targets: List<Label>)

public data class JvmBinaryJarsResult(val items: List<JvmBinaryJarsItem>)

public data class JvmBinaryJarsItem(val target: Label, val jars: List<String>)
