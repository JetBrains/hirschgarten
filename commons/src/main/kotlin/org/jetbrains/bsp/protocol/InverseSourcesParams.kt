package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class InverseSourcesParams(val originId: String?, val files: List<Path>)
