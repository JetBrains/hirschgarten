package org.jetbrains.bazel.sync.workspace.languages.scala

import java.nio.file.Path

data class ScalaSdk(val version: String, val compilerJars: List<Path>)
