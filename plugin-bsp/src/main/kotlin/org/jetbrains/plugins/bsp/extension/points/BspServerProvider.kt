package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName

import org.jetbrains.plugins.bsp.server.client.BspClient
import java.nio.file.Path

interface BspServerProvider {
    companion object {
        val ep = ExtensionPointName.create<BspServerProvider>("org.jetbrains.bsp.startBuiltInServerExtension")
        fun getBspServer() = ep.extensionList.firstOrNull()
    }

    fun getConnection(
        installationDirectory: Path,
        metricsFile: Path?,
        workspace: Path,
        bspClient: BspClient
    ): GenericConnection?
}
