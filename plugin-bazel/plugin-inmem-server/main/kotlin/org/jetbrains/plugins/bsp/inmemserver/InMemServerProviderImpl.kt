package org.jetbrains.plugins.bsp.inmemserver

import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.bazel.bsp.connection.DotBazelBspCreator
import org.jetbrains.bsp.inmem.Connection
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.extension.points.BspServerProvider
import org.jetbrains.plugins.bsp.extension.points.GenericConnection
import org.jetbrains.plugins.bsp.server.client.BspClient
import java.nio.file.Path
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.rootDir

class InMemServerProviderImpl: BspServerProvider {
    override fun getConnection(
        project: Project,
        metricsFile: Path?,
        bspClient: BspClient
    ): GenericConnection {
        return object : GenericConnection {
            val installationDirectory = project.rootDir.toNioPath()
            val conn = Connection(installationDirectory, metricsFile, installationDirectory, bspClient)
            val projectPath = VfsUtil.findFile(installationDirectory, true) ?: error("Project doesn't exist")
            init {
                DotBazelBspCreator(projectPath).create()
            }

            override val server: JoinedBuildServer
                get() = conn.clientLauncher.remoteProxy

            override fun shutdown() {
                conn.stop()
            }
        }
    }
}