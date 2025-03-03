package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class ScalaMainClassesItem(val target: Label, val classes: List<ScalaMainClass>)
