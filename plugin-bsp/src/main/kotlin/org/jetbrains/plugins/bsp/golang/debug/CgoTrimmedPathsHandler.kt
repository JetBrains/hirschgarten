package org.jetbrains.plugins.bsp.golang.debug

interface CgoTrimmedPathsHandler {
    fun matchesCgoTrimmedPath(path: String): Boolean

    fun normalizeCgoTrimmedPath(path: String): String
}
