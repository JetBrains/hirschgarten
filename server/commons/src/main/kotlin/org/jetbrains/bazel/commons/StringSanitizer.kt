package org.jetbrains.bazel.commons

fun String.escapeNewLines(): String = this.replace("\n", "\\n")
