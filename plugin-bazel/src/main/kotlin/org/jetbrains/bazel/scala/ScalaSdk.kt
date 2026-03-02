package org.jetbrains.bazel.scala.sdk

import java.net.URI

internal data class ScalaSdk(
  val name: String,
  val scalaVersion: String,
  val sdkJars: List<URI>,
)
