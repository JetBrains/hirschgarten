package org.jetbrains.plugins.bsp.impl.server.client

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.extension.points.GenericConnection
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
