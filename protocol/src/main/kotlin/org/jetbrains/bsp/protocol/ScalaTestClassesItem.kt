package org.jetbrains.bsp.protocol

data class ScalaTestClassesItem(
  val target: BuildTargetIdentifier,
  val classes: List<String>,
  val framework: String? = null,
)
