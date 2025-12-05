package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.legacy

import com.intellij.openapi.components.serviceAsync
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.WorkspaceModelUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.CompiledSourceCodeInsideJarExcludeTransformer
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncProgressReporter
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.JvmModuleEntity
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.JvmResourceId
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource
import kotlin.collections.orEmpty

private const val MAX_REPLACE_WSM_ATTEMPTS = 3

class LegacyWorkspaceModelApplicator(
  val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity>,
) {
  suspend fun execute(ctx: SyncContext, progress: SyncProgressReporter) {
    val entityStorage = MutableEntityStorage.create()

    val legacyModel = progress.task.withTask(
      taskId = "prepare_legacy_model",
      message = "Preparing legacy model",
    ) {
      LegacyModelConverter(storage).convert(ctx)
    }

    val workspaceContext = ctx.project.connection.runWithServer { server -> server.workspaceContext() }
    updateInternalModelSubtask(
      ctx = ctx,
      progress = progress,
      projectDetails = ProjectDetails(
        targetIds = legacyModel.targets.map { it.id },
        targets = legacyModel.targets.toSet(),
        libraries = legacyModel.libraries,
        workspaceContext = workspaceContext
      ),
      entityStorage = entityStorage
    )

    progress.task.withTask(
      taskId = "apply-changes-on-workspace-model",
      message = BazelPluginBundle.message("console.task.model.apply.changes"),
    ) {
      val workspaceModel = ctx.project.serviceAsync<WorkspaceModel>() as WorkspaceModelImpl
      workspaceModel.updateWithRetry(
        BazelPluginBundle.message("console.task.model.apply.changes.attempt.0.1.wsm", 0, 0),
        MAX_REPLACE_WSM_ATTEMPTS,
      ) { builder ->
        builder.replaceBySource(
          sourceFilter = {
            it is BazelEntitySource
          },
          replaceWith = entityStorage,
        )
      }
    }
  }

  private suspend fun updateInternalModelSubtask(
    ctx: SyncContext,
    progress: SyncProgressReporter,
    projectDetails: ProjectDetails,
    entityStorage: MutableEntityStorage,
  ) {
    val project = ctx.project
    progress.task.withTask(
      taskId = "calculate-project-structure",
      message = BazelPluginBundle.message("console.task.model.calculate.structure"),
    ) {
      coroutineScope {
        val projectBasePath = project.rootDir.toNioPath()
        val libraryGraph = LibraryGraph(projectDetails.libraries.orEmpty())

        val libraries =
          bspTracer.spanBuilder("create.libraries.ms").use {
            libraryGraph.createLibraries(project)
          }

        val libraryModules =
          bspTracer.spanBuilder("create.library.modules.ms").use {
            libraryGraph.createLibraryModules(project, projectDetails.defaultJdkName)
          }

        val targetIdToModuleDetails =
          bspTracer.spanBuilder("create.module.details.ms").use {
            val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, libraryGraph)
            projectDetails.targetIds.associateWith { transformer.moduleDetailsForTargetId(it) }
          }

        val targetIdToModuleEntitiesMap =
          bspTracer.spanBuilder("create.target.id.to.module.entities.map.ms").use {
            val syncedTargetIdToTargetInfo =
              (projectDetails.targets).associateBy { it.id }
            val targetIdToModuleEntityMap =
              TargetIdToModuleEntitiesMap(
                projectDetails = projectDetails,
                targetIdToModuleDetails = targetIdToModuleDetails,
                targetIdToTargetInfo = syncedTargetIdToTargetInfo,
                // TODO: remove usage, https://youtrack.jetbrains.com/issue/BAZEL-2015
                fileToTargetWithoutLowPrioritySharedSources = mapOf(), // TODO
                projectBasePath = projectBasePath,
                project = project,
              )
            targetIdToModuleEntityMap
          }

        val modulesToLoad = targetIdToModuleEntitiesMap.values.flatten().distinctBy { module -> module.getModuleName() }

        val compiledSourceCodeInsideJarToExclude =
          bspTracer.spanBuilder("calculate.non.generated.class.files.to.exclude").use {
            if (BazelFeatureFlags.excludeCompiledSourceCodeInsideJars) {
              CompiledSourceCodeInsideJarExcludeTransformer().transform(
                targetIdToModuleDetails.values,
                projectDetails.libraries.orEmpty(),
              )
            } else {
              null
            }
          }

        bspTracer.spanBuilder("load.modules.ms").use {
          val workspaceModel = project.serviceAsync<WorkspaceModel>()
          val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()

          val workspaceModelUpdater =
            WorkspaceModelUpdater(
              workspaceEntityStorageBuilder = entityStorage,
              virtualFileUrlManager = virtualFileUrlManager,
              projectBasePath = projectBasePath,
              project = project,
              importIjars = projectDetails.workspaceContext?.importIjars ?: false,
            )

          workspaceModelUpdater.load(modulesToLoad, libraries, libraryModules)
          compiledSourceCodeInsideJarToExclude?.let { workspaceModelUpdater.loadCompiledSourceCodeInsideJarExclude(it) }
        }
      }
    }
  }
}
