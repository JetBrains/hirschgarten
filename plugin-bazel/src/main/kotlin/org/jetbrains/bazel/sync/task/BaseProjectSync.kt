package org.jetbrains.bazel.sync.task

import com.intellij.openapi.project.Project
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.sync.BaseTargetInfo
import org.jetbrains.bazel.sync.BaseTargetInfos
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.ui.console.ids.BASE_PROJECT_SYNC_SUBTASK_ID
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.SourcesItem
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
import kotlin.collections.orEmpty

class BaseProjectSync(private val project: Project) {
  suspend fun execute(
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
    server: JoinedBuildServer,
    taskId: String,
  ): BaseTargetInfos =
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = BASE_PROJECT_SYNC_SUBTASK_ID,
      message = BspPluginBundle.message("console.task.base.sync"),
    ) {
      coroutineScope {
        val buildTargets = queryWorkspaceBuildTargets(server, syncScope, buildProject, taskId)
        val allTargetIds = buildTargets.calculateAllTargetIds()

        val sourcesResult = asyncQuery("buildTarget/sources") { server.buildTargetSources(SourcesParams(allTargetIds)) }
        val resourcesResult =
          asyncQuery("buildTarget/resources") {
            server.buildTargetResources(ResourcesParams(allTargetIds))
          }

        BaseTargetInfos(
          allTargetIds = allTargetIds,
          infos = calculateBaseTargetInfos(buildTargets, sourcesResult.await(), resourcesResult.await()),
        )
      }
    }

  private suspend fun queryWorkspaceBuildTargets(
    server: JoinedBuildServer,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
    taskId: String,
  ): List<BuildTarget> =
    coroutineScope {
      val result =
        // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1237
        // PartialProjectSync is used only in ResyncTargetAction, which is visible only for bazel-bsp project
        if (syncScope is PartialProjectSync) {
          query("workspace/buildTargetsPartial") {
            server.workspaceBuildTargetsPartial(WorkspaceBuildTargetsPartialParams(syncScope.targetsToSync))
          }
        } else if (syncScope is FirstPhaseSync) {
          // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1555
          query(
            "workspace/buildTargetsFirstPhase",
          ) { server.workspaceBuildTargetsFirstPhase(WorkspaceBuildTargetsFirstPhaseParams(taskId)) }
        } else if (buildProject) {
          query("workspace/buildAndGetBuildTargets") { server.workspaceBuildAndGetBuildTargets() }
        } else {
          query("workspace/buildTargets") { server.workspaceBuildTargets() }
        }
      result.targets
    }

  private fun List<BuildTarget>.calculateAllTargetIds(): List<BuildTargetIdentifier> = map { it.id }

  private fun calculateBaseTargetInfos(
    buildTargets: List<BuildTarget>,
    sourcesResult: SourcesResult,
    resourcesResult: ResourcesResult?,
  ): List<BaseTargetInfo> {
    val sourcesIndex = sourcesResult.toSourcesIndex()
    val resourcesIndex = resourcesResult?.toResourcesIndex().orEmpty()

    return buildTargets.map {
      BaseTargetInfo(
        target = it,
        sources = sourcesIndex[it.id].orEmpty(),
        resources = resourcesIndex[it.id].orEmpty(),
      )
    }
  }

  private fun SourcesResult.toSourcesIndex(): Map<BuildTargetIdentifier, List<SourcesItem>> = items.groupBy { it.target }

  private fun ResourcesResult.toResourcesIndex(): Map<BuildTargetIdentifier, List<ResourcesItem>> = items.groupBy { it.target }
}
