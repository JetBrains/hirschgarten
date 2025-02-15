package org.jetbrains.plugins.bsp.scala.sdk

data class ScalaSdk(
  val name: String,
  val scalaVersion: String,
  val sdkJars: List<String>,
)
