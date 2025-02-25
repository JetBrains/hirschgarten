package org.jetbrains.bsp.protocol

data class ScalaWorkspaceEdit(val changes: List<ScalaTextEdit>)
