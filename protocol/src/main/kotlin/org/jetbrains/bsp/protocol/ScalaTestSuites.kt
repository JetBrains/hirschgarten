package org.jetbrains.bsp.protocol

data class ScalaTestSuites(
  val suites: List<ScalaTestSuiteSelection>,
  val jvmOptions: List<String>,
  val environmentVariables: List<String>,
)
