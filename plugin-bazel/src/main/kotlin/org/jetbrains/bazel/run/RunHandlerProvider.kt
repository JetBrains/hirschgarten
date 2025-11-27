package org.jetbrains.bazel.run

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTarget

interface RunHandlerProvider {
  /**
   * Returns the unique ID of this {@link BspRunHandlerProvider}. The ID is
   * used to store configuration settings and must not change between plugin versions.
   */
  val id: String

  /**
   * Creates a {@link BspRunHandler} for the given configuration.
   */
  fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler

  /**
   * Returns true if this provider can create a {@link BspRunHandler} for running the given targets.
   */
  fun canRun(targetInfos: List<BuildTarget>): Boolean

  /**
   * Returns true if this provider can create a {@link BspRunHandler} for debugging the given targets.
   */
  fun canDebug(targetInfos: List<BuildTarget>): Boolean

  companion object {
    val ep: ExtensionPointName<RunHandlerProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.runHandlerProvider")

    /** Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets */
    fun getRunHandlerProvider(targetInfos: List<BuildTarget>, isDebug: Boolean = false): RunHandlerProvider? =
      ep.extensionList.firstOrNull {
        if (isDebug) {
          it.canDebug(targetInfos)
        } else {
          it.canRun(targetInfos)
        }
      }

    /** Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets.
     *  Needs to query WM for Build Target Infos. */
    fun getRunHandlerProvider(project: Project, targets: List<Label>): RunHandlerProvider {
      val targetUtils = project.targetUtils
      val targetInfos =
        targets.mapNotNull {
          targetUtils.getBuildTargetForLabel(it)
        }
      if (targetInfos.size != targets.size) {
        thisLogger().warn("Some targets could not be found: ${targets - targetInfos.map { it.id }.toSet()}")
      }

      require(targetInfos.isNotEmpty()) { "targetInfos should not be empty" }

      return getRunHandlerProvider(targetInfos)
        ?: throw IllegalArgumentException("No BspRunHandlerProvider found for targets: $targets")
    }

    fun getRunHandlerProviderOrNull(project: Project, targets: List<Label>): RunHandlerProvider? {
      val targetUtils = project.targetUtils
      val targetInfos = targets.mapNotNull { targetUtils.getBuildTargetForLabel(it) }
      if (targetInfos.isEmpty()) return null
      return getRunHandlerProvider(targetInfos)
    }

    /** Finds a BspRunHandlerProvider by its unique ID */
    fun findRunHandlerProvider(id: String): RunHandlerProvider? = ep.extensionList.firstOrNull { it.id == id }
  }
}
