package org.jetbrains.bazel.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier

internal class InvalidTargetsProjectSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val bazelInvalidTargetsService = BazelInvalidTargetsService.getInstance(environment.project)
    val invalidTargetsResult =
      query("workspace/invalidTargets") {
        environment.server.workspaceInvalidTargets()
      }.targets
    bazelInvalidTargetsService.setInvalidTargets(invalidTargetsResult)

    if (invalidTargetsResult.isNotEmpty()) {
      BazelBalloonNotifier.warn(
        BazelPluginBundle.message("widget.collect.targets.not.imported.properly.title"),
        BazelPluginBundle.message("widget.collect.targets.not.imported.properly.message"),
      )
    }
  }
}

@Serializable
internal data class BazelInvalidTargetsServiceState(val invalidTargets: List<Label> = emptyList())

@State(
  name = "BazelInvalidTargetsService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class BazelInvalidTargetsService : SerializablePersistentStateComponent<BazelInvalidTargetsServiceState>(BazelInvalidTargetsServiceState()) {
  companion object {
    internal fun getInstance(project: Project): BazelInvalidTargetsService = project.service<BazelInvalidTargetsService>()
  }

  fun setInvalidTargets(invalidTargets: List<Label>) {
    state = state.copy(invalidTargets = invalidTargets)
  }
}

val Project.invalidTargets: List<Label>
  get() = BazelInvalidTargetsService.getInstance(this).state.invalidTargets
