package org.jetbrains.bsp.protocol

data class ScalaMainClassesResult(val items: List<ScalaMainClassesItem>, val originId: String? = null)
