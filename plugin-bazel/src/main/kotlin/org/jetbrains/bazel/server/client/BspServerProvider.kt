package org.jetbrains.bazel.server.client

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.nio.file.Path

interface BspServerProvider {
  companion object {
    val ep = ExtensionPointName.create<BspServerProvider>("org.jetbrains.bsp.startBuiltInServerExtension")

    fun getBspServer() = ep.extensionList.firstOrNull()
  }

  fun getConnection(
    project: Project,
    metricsFile: Path?,
    bspClient: BspClient,
  ): GenericConnection?
}

interface GenericConnection {
  val server: JoinedBuildServer

  fun shutdown()
}
