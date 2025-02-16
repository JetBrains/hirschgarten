package org.jetbrains.plugins.bsp.sync.task

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import com.intellij.openapi.project.Project
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.sync.BaseTargetInfo
import org.jetbrains.plugins.bsp.sync.BaseTargetInfos
import org.jetbrains.plugins.bsp.sync.scope.FirstPhaseSync
import org.jetbrains.plugins.bsp.sync.scope.PartialProjectSync
import org.jetbrains.plugins.bsp.sync.scope.ProjectSyncScope
import org.jetbrains.plugins.bsp.ui.console.ids.BASE_PROJECT_SYNC_SUBTASK_ID
import org.jetbrains.plugins.bsp.ui.console.syncConsole
import org.jetbrains.plugins.bsp.ui.console.withSubtask
import kotlin.collections.orEmpty

class BaseProjectSync(private val project: Project) {
  suspend fun execute(
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
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
          asyncQueryIf(capabilities.resourcesProvider == true, "buildTarget/resources") {
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

  private fun SourcesResult.toSourcesIndex(): Map<BuildTargetIdentifier, List<SourcesItem>> = items.orEmpty().groupBy { it.target }

  private fun ResourcesResult.toResourcesIndex(): Map<BuildTargetIdentifier, List<ResourcesItem>> = items.orEmpty().groupBy { it.target }
}
