package org.jetbrains.plugins.bsp.python

import ch.epfl.scala.bsp4j.DependencySourcesItem

data class PythonSdk(
  val name: String,
  val interpreterUri: String,
  val dependencies: List<DependencySourcesItem>,
)
