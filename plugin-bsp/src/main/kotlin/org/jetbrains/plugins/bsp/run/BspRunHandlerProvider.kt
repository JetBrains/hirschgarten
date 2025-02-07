package org.jetbrains.plugins.bsp.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.target.TargetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

interface BspRunHandlerProvider {
  /**
   * Returns the unique ID of this {@link BspRunHandlerProvider}. The ID is
   * used to store configuration settings and must not change between plugin versions.
   */
  val id: String

  /**
   * Creates a {@link BspRunHandler} for the given configuration.
   */
  fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler

  /**
   * Returns true if this provider can create a {@link BspRunHandler} for running the given targets.
   */
  fun canRun(targetInfos: List<BuildTargetInfo>): Boolean

  /**
   * Returns true if this provider can create a {@link BspRunHandler} for debugging the given targets.
   */
  fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean

  companion object {
    val ep: ExtensionPointName<BspRunHandlerProvider> =
      ExtensionPointName.create("org.jetbrains.bsp.bspRunHandlerProvider")

    /** Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets */
    fun getRunHandlerProvider(targetInfos: List<BuildTargetInfo>, isDebug: Boolean = false): BspRunHandlerProvider? =
      ep.extensionList.firstOrNull {
        if (isDebug) {
          it.canDebug(targetInfos)
        } else {
          it.canRun(targetInfos)
        }
      }

    /** Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets.
     *  Needs to query WM for Build Target Infos. */
    fun getRunHandlerProvider(project: Project, targets: List<BuildTargetIdentifier>): BspRunHandlerProvider {
      val targetInfos =
        targets.mapNotNull {
          project.service<TargetUtils>().getBuildTargetInfoForId(it)
        }
      if (targetInfos.size != targets.size) {
        thisLogger().warn("Some targets could not be found: ${targets - targetInfos.map { it.id }.toSet()}")
      }

      return getRunHandlerProvider(targetInfos)
        ?: throw IllegalArgumentException("No BspRunHandlerProvider found for targets: $targets")
    }

    /** Finds a BspRunHandlerProvider by its unique ID */
    fun findRunHandlerProvider(id: String): BspRunHandlerProvider? = ep.extensionList.firstOrNull { it.id == id }
  }
}
