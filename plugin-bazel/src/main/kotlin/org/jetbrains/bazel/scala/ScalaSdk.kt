package org.jetbrains.bazel.scala.sdk

data class ScalaSdk(
  val name: String,
  val scalaVersion: String,
  val sdkJars: List<String>,
)
