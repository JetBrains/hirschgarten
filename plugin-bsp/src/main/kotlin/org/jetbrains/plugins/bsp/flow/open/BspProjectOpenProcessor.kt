package org.jetbrains.plugins.bsp.flow.open

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.config.BazelBspConstants
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionFilesProvider
import javax.swing.Icon

// TODO: move to a better package
public data class BuildToolId(public val id: String)

// TODO: move to a better package
public interface WithBuildToolId {
  public val buildToolId: BuildToolId
}

// TODO: move to a better package
internal fun <T : WithBuildToolId> ExtensionPointName<T>.withBuildToolId(buildToolId: BuildToolId): T? =
  this.extensions.find { it.buildToolId == buildToolId }

internal fun <T : WithBuildToolId> ExtensionPointName<T>.withBuildToolIdOrDefault(buildToolId: BuildToolId): T =
  this.extensions.find { it.buildToolId == buildToolId }
    ?: withBuildToolId(BuildToolId("bsp"))
    ?: error("Missing default implementation for extension: ${this.javaClass.name}")

public interface BspProjectOpenProcessorExtension : WithBuildToolId {
  /**
   * When a project is opened for the first time [com.intellij.projectImport.ProjectOpenProcessor.canOpenProject]
   * is executed on each registered processor. BSP plugin implements one as well ([org.jetbrains.plugins.bsp.flow.open.BspProjectOpenProcessor]).
   * Basically it checks if there are available [connection files](https://build-server-protocol.github.io/docs/overview/server-discovery#default-locations-for-bsp-connection-files)
   * and returns `True` if there is at least one connection file.
   *
   * In case you want to support a particular build tool in a more "native" way a dedicated [com.intellij.projectImport.ProjectOpenProcessor]
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
      ExtensionPointName.create<BspProjectOpenProcessorExtension>("com.intellij.bspProjectOpenProcessorExtension")
  }
}

internal class BspProjectOpenProcessor : BaseBspProjectOpenProcessor(BuildToolId("bsp")) {
  override val name: String = BspPluginBundle.message("plugin.name")

  override val icon: Icon = BspPluginIcons.bsp

  override fun canOpenProject(file: VirtualFile): Boolean {
    val buildToolIds = file.collectBuildToolIdsFromConnectionFiles()

    return buildToolIds.any { it.shouldBspProjectOpenProcessorBeAvailable() }
  }

  private fun VirtualFile.collectBuildToolIdsFromConnectionFiles(): List<BuildToolId> =
    BspConnectionFilesProvider(this)
      .connectionFiles
      .mapNotNull { it.bspConnectionDetails?.name }
      .map { BuildToolId(it) }

  private fun BuildToolId.shouldBspProjectOpenProcessorBeAvailable(): Boolean =
    BspProjectOpenProcessorExtension.ep.withBuildToolId(this)?.shouldBspProjectOpenProcessorBeAvailable ?: true
}

// TODO should be moved to the bazel plugin
public class BazelBspProjectOpenProcessor : BaseBspProjectOpenProcessor(BuildToolId("bazelbsp")) {
  override val icon: Icon = BspPluginIcons.bazel

  override val name: String = "Bazel"

  override fun canOpenProject(file: VirtualFile): Boolean =
    file.children.any { it.name in BazelBspConstants.BUILD_FILE_NAMES }
}

public class BazelBspProjectOpenProcessorExtension : BspProjectOpenProcessorExtension {
  override val buildToolId: BuildToolId = BuildToolId("bazelbsp")

  override val shouldBspProjectOpenProcessorBeAvailable: Boolean = false
}
