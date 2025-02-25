package org.jetbrains.bsp.protocol

data class ScalaTextEdit(val range: Range, val newText: String)
