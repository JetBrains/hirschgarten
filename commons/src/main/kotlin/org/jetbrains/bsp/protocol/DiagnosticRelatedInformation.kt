package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class DiagnosticRelatedInformation(val location: Location, val message: String)
