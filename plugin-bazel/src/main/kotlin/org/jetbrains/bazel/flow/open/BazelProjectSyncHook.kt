package org.jetbrains.bazel.flow.open

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.flow.open.ProjectSyncHook
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.services.InvalidTargetsProviderExtension
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier

internal class BazelProjectSyncHook : ProjectSyncHook {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun onSync(project: Project, server: BspServer) {
    val bazelInvalidTargetsService = BazelInvalidTargetsService.getInstance(project)
    bazelInvalidTargetsService.invalidTargets = server.workspaceInvalidTargets().get().targets

    if (bazelInvalidTargetsService.invalidTargets.isNotEmpty()) {
        BspBalloonNotifier.warn(
          BazelPluginBundle.message("widget.collect.targets.not.imported.properly.title"),
          BazelPluginBundle.message("widget.collect.targets.not.imported.properly.message"),
        )
    }
  }
}

internal data class BazelInvalidTargetsServiceState(
  var invalidTargets: List<String> = emptyList()
)

@State(
  name = "BazelInvalidTargetsService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class BazelInvalidTargetsService: PersistentStateComponent<BazelInvalidTargetsServiceState> {
  internal var invalidTargets: List<BuildTargetIdentifier> = emptyList()

  override fun getState(): BazelInvalidTargetsServiceState? =
    BazelInvalidTargetsServiceState(invalidTargets.map { it.uri })
      .takeIf { it.invalidTargets.isNotEmpty() }

  override fun loadState(state: BazelInvalidTargetsServiceState) {
    invalidTargets = state.invalidTargets.map { BuildTargetIdentifier(it) }
  }

  companion object {
    internal fun getInstance(project: Project): BazelInvalidTargetsService =
      project.service<BazelInvalidTargetsService>()
  }
}

// quite temporary as well
internal class BazelInvalidTargetsProviderExtension: InvalidTargetsProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun provideInvalidTargets(project: Project): List<BuildTargetIdentifier> =
    BazelInvalidTargetsService.getInstance(project).invalidTargets
}
