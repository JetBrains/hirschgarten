package org.jetbrains.plugins.bsp.scala.sdk

public data class ScalaSdk(
  val name: String,
  val scalaVersion: String,
  val sdkJars: List<String>,
)
