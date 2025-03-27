package org.jetbrains.bazel.scala.sdk

import java.nio.file.Path

data class ScalaSdk(
  val name: String,
  val scalaVersion: String,
  val sdkJars: List<Path>,
)
