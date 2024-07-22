package org.jetbrains.plugins.bsp.extension.points

import org.jetbrains.bsp.protocol.JoinedBuildServer

interface GenericConnection {
    val server: JoinedBuildServer
    fun shutdown()
}