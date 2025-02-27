package org.jetbrains.bsp.protocol

data class ScalaTestSuiteSelection(val className: String, val tests: List<String>)
