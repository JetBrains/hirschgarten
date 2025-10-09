package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class CachedTestLog(val originId: String, val testLog: Path)
