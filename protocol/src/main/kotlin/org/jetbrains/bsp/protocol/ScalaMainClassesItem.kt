package org.jetbrains.bsp.protocol

data class ScalaMainClassesItem(val target: BuildTargetIdentifier, val classes: List<ScalaMainClass>)
