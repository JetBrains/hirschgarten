package org.jetbrains.bazel.scala.sdk

import org.jetbrains.annotations.ApiStatus
import java.net.URI

@ApiStatus.Internal
data class ScalaSdk(
  val name: String,
  val scalaVersion: String,
  val sdkJars: List<URI>,
)
