package org.jetbrains.bazel.scala.sdk

import org.jetbrains.bazel.annotations.PublicApi
import java.net.URI

@PublicApi
data class ScalaSdk(
  val name: String,
  val scalaVersion: String,
  val sdkJars: List<URI>,
)
