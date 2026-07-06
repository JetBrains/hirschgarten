package org.jetbrains.bazel.golang.sync

import com.goide.inspections.GoWrongSdkConfigurationNotificationProvider
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.GoSdkUtil
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.golang.workspace.GO_WORKSPACE_MODULE_NAME
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.utils.filterPathsThatDontContainEachOther
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelGoTargetEntity
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

internal class GoWorkspaceImporter : BazelWorkspaceImporter, BazelWorkspaceImporter.Named {
  lateinit var goTargets: Map<WorkspaceTargetKey, GoBuildTarget>

  override val importerName: @NlsContexts.ProgressTitle String
    get() = BazelPluginBundle.message("console.task.model.go.importer")

  override suspend fun import(
    context: WorkspaceImporterContext,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> {
    when (phase) {
      WorkspaceImporterPhase.Initialize -> {
        if (!BazelFeatureFlags.isGoSupportEnabled) return Result.success(WorkspaceImporterResult.Abort)
        goTargets = snapshot.targets.mapNotNull { (key, target) ->
          key to (extractGoBuildTarget(target.rawBuildTarget) ?: return@mapNotNull null)
        }.toMap()
        if (goTargets.isEmpty()) return Result.success(WorkspaceImporterResult.Abort)
      }

      is WorkspaceImporterPhase.WorkspaceApply -> {
        onWorkspaceApply(context, phase.builder, phase.entitySource)
      }

      WorkspaceImporterPhase.PostProcessing -> {
        onPostProcessing(context)
      }

      else -> {}
    }
    return Result.success(WorkspaceImporterResult.Success)
  }

  private fun onWorkspaceApply(
    context: WorkspaceImporterContext,
    builder: MutableEntityStorage,
    entitySource: EntitySource,
  ) {
    addGoWorkspaceModule(builder, context, entitySource)
    addGoPackageEntities(builder, context, entitySource)
  }

  /**
   * Go plugin expects all sources to be under a content root.
   * Otherwise, e.g., Go SDK references aren't resolved, and source files aren't indexed.
   * By the way, we *don't* need to add source roots, those are specific to Java/Python and are ignored by the Go plugin.
   */
  private fun addGoWorkspaceModule(
    builder: MutableEntityStorage,
    context: WorkspaceImporterContext,
    entitySource: EntitySource,
  ) {
    val project = context.project

    val workspacePath = project.rootDir.toNioPath()

    val goSourcesParentDirectories = goTargets.values
      .flatMap { it.sources }
      .filter { it.startsWith(workspacePath) }  // External files are handled in GoExternalLibraryManager
      .map { source -> source.parent }
      .toSet()
      .filterPathsThatDontContainEachOther()
    if (goSourcesParentDirectories.isEmpty()) return
    val contentRoots = goSourcesParentDirectories.map { directory: Path ->
      ContentRootEntity(
        url = directory.toVirtualFileUrl(context.vfuManager),
        entitySource = entitySource,
        excludedPatterns = emptyList(),
      )
    }

    val moduleEntity =
      ModuleEntity(
        name = GO_WORKSPACE_MODULE_NAME,
        dependencies = listOf(ModuleSourceDependency),
        entitySource = entitySource,
      ) {
        this.contentRoots = contentRoots
      }
    builder.addEntity(moduleEntity)
  }

  private fun addGoPackageEntities(
    builder: MutableEntityStorage,
    context: WorkspaceImporterContext,
    entitySource: EntitySource,
  ) {
    val inferredImportPath = inferImportPath(goTargets)

    val targetsByImportPath = goTargets.entries.groupBy { inferredImportPath[it.key] ?: it.value.importPath }

    for ((importPath, goTargets) in targetsByImportPath) {
      // Group targets with the same importpath, see doc for BazelGoPackageEntity
      val packageEntity = builder addEntity BazelGoPackageEntity(
        importPath = importPath,
        sources = goTargets.flatMap { it.value.sources }.distinct().map { it.toVirtualFileUrl(context.vfuManager) },
        entitySource = entitySource,
      )
      val importPathId = packageEntity.symbolicId

      for (target in goTargets) {
        builder addEntity BazelGoTargetEntity(
          _targetKey = WorkspaceModelTargetKey.of(target.key),
          importPath = importPathId,
          entitySource = entitySource,
        )
      }
    }
  }

  private fun inferImportPath(targetsToImport: Map<WorkspaceTargetKey, GoBuildTarget>): Map<WorkspaceTargetKey, String> {
    val inferredImportPath = hashMapOf<WorkspaceTargetKey, String>()

    for ((key, goTarget) in targetsToImport) {
      val embedList = goTarget.embed

      // From https://github.com/bazel-contrib/rules_go/blob/master/docs/go/core/rules.md#go_library-importpath:
      // importpath must either be specified in go_library or inherited from one of the libraries in embed.
      val importPath = if (goTarget.importPath.isNotEmpty()) {
        goTarget.importPath
      }
      else {
        val inferred = embedList.asSequence().map { embed ->
          targetsToImport[embed]?.importPath
        }.firstOrNull { !it.isNullOrEmpty() }.orEmpty()
        if (inferred.isNotEmpty()) inferredImportPath[key] = inferred
        inferred
      }

      if (importPath.isNotEmpty()) {
        // go_source doesn't have importpath (https://github.com/bazel-contrib/rules_go/blob/master/docs/go/core/rules.md#go_source)
        // But we can infer it from the library that embeds it
        for (embed in embedList) {
          if (targetsToImport[embed]?.importPath?.isEmpty() == true) {
            inferredImportPath[embed] = importPath
          }
        }
      }
    }
    return inferredImportPath
  }

  private suspend fun onPostProcessing(context: WorkspaceImporterContext) {
    calculateAndAddGoSdk(context.progressReporter, context.project, context.taskId)
    GoWrongSdkConfigurationNotificationProvider.disableNotification(context.project)
  }

  private suspend fun calculateAndAddGoSdk(
    reporter: SequentialProgressReporter,
    project: Project,
    taskId: TaskId,
  ) = project.syncConsole.withSubtask(
    reporter = reporter,
    subtaskId = taskId.subTask("calculate-and-add-go-sdk"),
    text = BazelPluginBundle.message("console.task.model.calculate.add.go.fetched.sdk"),
  ) {
    goTargets
      .values
      .firstNotNullOfOrNull { it.sdkHomePath }
      .let { it ?: GoSdkUtil.suggestSdkDirectory() }
      ?.let { path -> GoSdk.fromHomePath(path.toString()) }
      ?.setAsUsed(project)
  }

  private suspend fun GoSdk.setAsUsed(project: Project) {
    val goSdkService = GoSdkService.getInstance(project)
    edtWriteAction { goSdkService.setSdk(this) }
  }
}
