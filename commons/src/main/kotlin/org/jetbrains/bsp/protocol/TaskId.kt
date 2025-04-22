package org.jetbrains.bsp.protocol

data class TaskId(val id: String, var parents: List<String> = emptyList())
