package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class PythonOptionsItem(val target: Label, val interpreterOptions: List<String>)
