package org.jetbrains.bsp.protocol

data class DiagnosticRelatedInformation(val location: Location, val message: String)
