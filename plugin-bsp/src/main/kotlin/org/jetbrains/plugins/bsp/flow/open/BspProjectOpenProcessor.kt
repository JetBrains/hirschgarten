package org.jetbrains.plugins.bsp.flow.open

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bsp.protocol.BSP_CONNECTION_DIR
import org.jetbrains.bsp.protocol.utils.parseBspConnectionDetails
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId
import org.jetbrains.plugins.bsp.server.connection.stateService
import javax.swing.Icon

interface BspProjectOpenProcessorExtension : WithBuildToolId {
  /**
   * When a project is opened for the first time [com.intellij.projectImport.ProjectOpenProcessor.canOpenProject]
   * is executed on each registered processor. BSP plugin implements one as well ([BspProjectOpenProcessor]).
   * Basically it checks if there are available [connection files](https://build-server-protocol.github.io/docs/overview/server-discovery#default-locations-for-bsp-connection-files)
   * and returns `True` if there is at least one connection file.
   *
   * In case you want to support a particular build tool in a more "native" way a dedicated [BaseBspProjectOpenProcessor]
   * is required. Then, if you want to dismiss BSP processor for connection files of your build tool
   * (so the user will not see BSP and your build tool options) [shouldBspProjectOpenProcessorBeAvailable] should be `False`.
   * If you still want to display the BSP processor option [shouldBspProjectOpenProcessorBeAvailable] should be `True`.
   *
   * NOTE: the mechanism works per build tool, so if there is a connection file of another build tool for which
   * [shouldBspProjectOpenProcessorBeAvailable] is `True` or an extension does not exist the BSP option will be still available.
   */
  val shouldBspProjectOpenProcessorBeAvailable: Boolean

  companion object {
    internal val ep =
      ExtensionPointName.create<BspProjectOpenProcessorExtension>("org.jetbrains.bsp.bspProjectOpenProcessorExtension")
  }
}

private val log = logger<BspProjectOpenProcessor>()

class BspProjectOpenProcessor : BaseBspProjectOpenProcessor(bspBuildToolId) {
  override val name: String = BspPluginBundle.message("plugin.name")

  override fun calculateProjectFolderToOpen(virtualFile: VirtualFile): VirtualFile =
    virtualFile
      .let { if (it.isFile) virtualFile.parent else virtualFile }
      .also { if (it.name != BSP_CONNECTION_DIR) error("No $BSP_CONNECTION_DIR folder found") }
      .parent

  override fun calculateBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    { project ->
      if (originalVFile.isBspConnectionFile()) {
        project.stateService.connectionFile = originalVFile
      }
    }

  private fun VirtualFile.isBspConnectionFile() = isFile && toBuildToolId() != null

  override val icon: Icon = BspPluginIcons.bsp

  override fun canOpenProject(file: VirtualFile): Boolean {
    val buildToolIds = file.collectBuildToolIdsFromConnectionFiles()
    log.debug("Detected build tool ids: $buildToolIds")

    return buildToolIds
      .any { it.shouldBspProjectOpenProcessorBeAvailable() }
      .also { log.debug("Will BspProjectOpenProcessor be available: $it") }
  }

  private fun VirtualFile.collectBuildToolIdsFromConnectionFiles(): List<BuildToolId> =
    when {
      this.isDirectory ->
        this
          .findChild(BSP_CONNECTION_DIR)
          ?.children
          ?.mapNotNull { it.toBuildToolId() }
          .orEmpty()
      else -> listOfNotNull(this.toBuildToolId())
    }

  private fun BuildToolId.shouldBspProjectOpenProcessorBeAvailable(): Boolean =
    BspProjectOpenProcessorExtension.ep.withBuildToolId(this)?.shouldBspProjectOpenProcessorBeAvailable ?: true
}

fun VirtualFile.toBuildToolId(): BuildToolId? = parseBspConnectionDetails()?.name?.let { BuildToolId(it) }
