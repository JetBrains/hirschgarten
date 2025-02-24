package org.jetbrains.bsp.protocol

data class ScalaTestClassesItem(
  val target: BuildTargetIdentifier,
  val framework: String? = null,
  val classes: List<String>,
)
