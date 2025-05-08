package org.jetbrains.bazel.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier

internal class InvalidTargetsProjectSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Collect invalid targets") {
      val bazelInvalidTargetsService = BazelInvalidTargetsService.getInstance(environment.project)
      val invalidTargetsResult =
        query("workspace/invalidTargets") {
          environment.server.workspaceInvalidTargets()
        }.targets
      bazelInvalidTargetsService.invalidTargets = invalidTargetsResult

      if (bazelInvalidTargetsService.invalidTargets.isNotEmpty()) {
        BazelBalloonNotifier.warn(
          BazelPluginBundle.message("widget.collect.targets.not.imported.properly.title"),
          BazelPluginBundle.message("widget.collect.targets.not.imported.properly.message"),
        )
      }
    }
  }
}

internal data class BazelInvalidTargetsServiceState(var invalidTargets: List<String> = emptyList())

@State(
  name = "BazelInvalidTargetsService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class BazelInvalidTargetsService : PersistentStateComponent<BazelInvalidTargetsServiceState> {
  internal var invalidTargets: List<Label> = emptyList()

  override fun getState(): BazelInvalidTargetsServiceState? =
    BazelInvalidTargetsServiceState(invalidTargets.map { it.toString() })
      .takeIf { it.invalidTargets.isNotEmpty() }

  override fun loadState(state: BazelInvalidTargetsServiceState) {
    invalidTargets = state.invalidTargets.map { Label.parse(it) }
  }

  companion object {
    internal fun getInstance(project: Project): BazelInvalidTargetsService = project.service<BazelInvalidTargetsService>()
  }
}

val Project.invalidTargets: List<Label>
  get() = BazelInvalidTargetsService.getInstance(this).invalidTargets
