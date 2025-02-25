package org.jetbrains.bsp.protocol

data class ScalaTestParams(val testClasses: List<ScalaTestClassesItem>? = null, val jvmOptions: List<String>? = null)
