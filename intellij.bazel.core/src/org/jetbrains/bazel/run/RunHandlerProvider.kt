package org.jetbrains.bazel.run

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
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
   * Implementations should generally handle [org.jetbrains.bazel.ui.gutters.NonImportedBuildTarget] as well.
   */
  fun canRun(project: Project, targets: List<BuildTarget>): Boolean

  fun canRunNonImported(project: Project, targets: List<Label>): Boolean = false

  companion object {
    val ep: ExtensionPointName<RunHandlerProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.runHandlerProvider")

    /** Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets */
    fun getRunHandlerProvider(project: Project, targets: List<BuildTarget>): RunHandlerProvider? =
      ep.extensionList.firstOrNull {
        it.canRun(project, targets)
      }

    fun getRunHandlerProviderOrNull(project: Project, targets: List<Label>): RunHandlerProvider? {
      val targetUtils = project.targetStorage
      val targetInfos = targets.mapNotNull { targetUtils.getTargetSummary(it) }
      return if (targetInfos.isNotEmpty()) {
        getRunHandlerProvider(project, targetInfos)
      }
      else {
        ep.extensionList.firstOrNull { it.canRunNonImported(project, targets) }
      }
    }

    /** Finds a BspRunHandlerProvider by its unique ID */
    fun findRunHandlerProvider(id: String): RunHandlerProvider? = ep.extensionList.firstOrNull { it.id == id }
  }
}
