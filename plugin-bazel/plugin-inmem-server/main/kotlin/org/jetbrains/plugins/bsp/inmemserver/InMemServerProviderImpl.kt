package org.jetbrains.plugins.bsp.inmemserver

import org.jetbrains.bsp.inmem.Connection
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.extension.points.BspServerProvider
import org.jetbrains.plugins.bsp.extension.points.GenericConnection
import org.jetbrains.plugins.bsp.server.client.BspClient
import java.nio.file.Path

class InMemServerProviderImpl: BspServerProvider {
    override fun getConnection(
        installationDirectory: Path,
        metricsFile: Path?,
        workspace: Path,
        bspClient: BspClient
    ): GenericConnection {
        return object : GenericConnection {
            val conn = Connection(installationDirectory, metricsFile, workspace, bspClient)
            override val server: JoinedBuildServer
                get() = conn.clientLauncher.remoteProxy

            override fun shutdown() {
                conn.stop()
            }
        }
    }
}