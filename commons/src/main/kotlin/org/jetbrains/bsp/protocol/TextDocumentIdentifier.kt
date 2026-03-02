package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
data class TextDocumentIdentifier(val path: Path)
