package org.jetbrains.plugins.bsp.flow.open

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bsp.protocol.BSP_CONNECTION_DIR
import org.jetbrains.bsp.protocol.utils.parseBspConnectionDetails
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.WithBuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolId
import javax.swing.Icon

public interface BspProjectOpenProcessorExtension : WithBuildToolId {
  /**
   * When a project is opened for the first time [com.intellij.projectImport.ProjectOpenProcessor.canOpenProject]
   * is executed on each registered processor. BSP plugin implements one as well ([org.jetbrains.plugins.bsp.flow.open.BspProjectOpenProcessor]).
   * Basically it checks if there are available [connection files](https://build-server-protocol.github.io/docs/overview/server-discovery#default-locations-for-bsp-connection-files)
   * and returns `True` if there is at least one connection file.
   *
   * In case you want to support a particular build tool in a more "native" way a dedicated [org.jetbrains.plugins.bsp.flow.open.BaseBspProjectOpenProcessor]
   * is required. Then, if you want to dismiss BSP processor for connection files of your build tool
   * (so the user will not see BSP and your build tool options) [shouldBspProjectOpenProcessorBeAvailable] should be `False`.
   * If you still want to display the BSP processor option [shouldBspProjectOpenProcessorBeAvailable] should be `True`.
   *
   * NOTE: the mechanism works per build tool, so if there is a connection file of another build tool for which
   * [shouldBspProjectOpenProcessorBeAvailable] is `True` or an extension does not exist the BSP option will be still available.
   */
  public val shouldBspProjectOpenProcessorBeAvailable: Boolean

  public companion object {
    internal val ep =
      ExtensionPointName.create<BspProjectOpenProcessorExtension>("org.jetbrains.bsp.bspProjectOpenProcessorExtension")
  }
}

internal class BspProjectOpenProcessor : BaseBspProjectOpenProcessor(bspBuildToolId) {
  override val name: String = BspPluginBundle.message("plugin.name")

  override val icon: Icon = BspPluginIcons.bsp

  override fun canOpenProject(file: VirtualFile): Boolean {
    val buildToolIds = file.collectBuildToolIdsFromConnectionFiles()

    return buildToolIds.any { it.shouldBspProjectOpenProcessorBeAvailable() }
  }

  private fun VirtualFile.collectBuildToolIdsFromConnectionFiles(): List<BuildToolId> =
    this.findChild(BSP_CONNECTION_DIR)
      ?.children
      ?.mapNotNull { it.parseBspConnectionDetails() }
      ?.map { BuildToolId(it.name) }
      .orEmpty()

  private fun BuildToolId.shouldBspProjectOpenProcessorBeAvailable(): Boolean =
    BspProjectOpenProcessorExtension.ep.withBuildToolId(this)?.shouldBspProjectOpenProcessorBeAvailable ?: true
}
