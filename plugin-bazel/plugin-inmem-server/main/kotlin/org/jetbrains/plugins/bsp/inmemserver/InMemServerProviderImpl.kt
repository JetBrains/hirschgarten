package org.jetbrains.plugins.bsp.inmemserver

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.bazel.bsp.connection.DotBazelBspCreator
import org.jetbrains.bazel.settings.bazelProjectSettings
import org.jetbrains.bsp.inmem.Connection
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.server.client.BspClient
import org.jetbrains.plugins.bsp.server.client.BspServerProvider
import org.jetbrains.plugins.bsp.server.client.GenericConnection
import java.nio.file.Path

class InMemServerProviderImpl : BspServerProvider {
  override fun getConnection(
    project: Project,
    metricsFile: Path?,
    bspClient: BspClient,
  ): GenericConnection =
    object : GenericConnection {
      val installationDirectory = project.rootDir.toNioPath()
      val conn =
        Connection(
          installationDirectory,
          metricsFile,
          project.bazelProjectSettings.projectViewPath?.toAbsolutePath(),
          installationDirectory,
          bspClient,
          propagateTelemetryContext = true,
        )
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
