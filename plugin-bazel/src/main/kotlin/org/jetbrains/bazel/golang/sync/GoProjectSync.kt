package org.jetbrains.bazel.golang.sync

import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.vgo.project.workspaceModel.VgoWorkspaceModelUpdater
import com.goide.vgo.project.workspaceModel.entities.VgoDependencyEntity
import com.goide.vgo.project.workspaceModel.entities.VgoStandaloneModuleEntity
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModule
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.includesGo
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import java.nio.file.Path

private const val GO_SOURCE_ROOT_TYPE = "go-source"
private const val GO_TEST_SOURCE_ROOT_TYPE = "go-test"
private const val GO_RESOURCE_ROOT_TYPE = "go-resource"

// looks strange, but GO plugin does not use this field extensively, so there is no GO-specific type available
// WEB_MODULE is the "recommended" one
private val GO_MODULE_TYPE = ModuleTypeId("WEB_MODULE")

class GoProjectSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.isGoSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    environment.withSubtask("Process Go targets") {
      // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1961
      val bspBuildTargets = environment.server.workspaceBuildTargets()
      val goTargets = bspBuildTargets.calculateGoTargets()
      val idToGoTargetMap = goTargets.associateBy({ it.id }, { it })
      val virtualFileUrlManager = environment.project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

      val moduleEntities =
        goTargets.map {
          val moduleName = it.id.formatAsModuleName(environment.project)
          val moduleSourceEntity = BazelModuleEntitySource(moduleName)

          val moduleEntity =
            addModuleEntityFromTarget(
              builder = environment.diff.workspaceModelDiff.mutableEntityStorage,
              target = it,
              moduleName = moduleName,
              entitySource = moduleSourceEntity,
              virtualFileUrlManager = virtualFileUrlManager,
              project = environment.project,
            )
          val vgoModule =
            prepareVgoModule(environment, it, moduleEntity.symbolicId, virtualFileUrlManager, idToGoTargetMap, moduleSourceEntity)
          environment.diff.workspaceModelDiff.mutableEntityStorage
            .addEntity(vgoModule)
          moduleEntity
        }

      environment.diff.workspaceModelDiff.addPostApplyAction {
        calculateAndAddGoSdk(environment.progressReporter, goTargets, environment.project, environment.taskId)
        restoreGoModulesRegistry(environment.project)
        enableGoSupportInTargets(moduleEntities, environment.project, environment.taskId)
      }
    }
  }

  private fun WorkspaceBuildTargetsResult.calculateGoTargets(): List<RawBuildTarget> = targets.filter { it.kind.includesGo() }

  private fun addModuleEntityFromTarget(
    builder: MutableEntityStorage,
    target: RawBuildTarget,
    moduleName: String,
    entitySource: BazelModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
    project: Project,
  ): ModuleEntity {
    val sourceContentRootEntities = getSourceContentRootEntities(target, entitySource, virtualFileUrlManager)
    val resourceContentRootEntities = getResourceContentRootEntities(target, entitySource, virtualFileUrlManager)
    return builder.addEntity(
      ModuleEntity(
        name = moduleName,
        dependencies =
          target.dependencies.map {
            ModuleDependency(
              module = ModuleId(it.formatAsModuleName(project)),
              exported = true,
              scope = DependencyScope.COMPILE,
              productionOnTest = true,
            )
          },
        entitySource = entitySource,
      ) {
        this.type = GO_MODULE_TYPE
        this.contentRoots = sourceContentRootEntities + resourceContentRootEntities
      },
    )
  }

  private fun getSourceContentRootEntities(
    target: RawBuildTarget,
    entitySource: BazelModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> =
    target.sources.map { source ->
      val sourceUrl = source.path.toVirtualFileUrl(virtualFileUrlManager)
      val sourceRootEntity =
        SourceRootEntity(
          url = sourceUrl,
          rootTypeId = SourceRootTypeId(inferRootType(target)),
          entitySource = entitySource,
        )
      ContentRootEntity(
        url = sourceUrl,
        excludedPatterns = ArrayList(),
        entitySource = entitySource,
      ) {
        this.excludedUrls = listOf()
        this.sourceRoots = listOf(sourceRootEntity)
      }
    }

  private fun inferRootType(buildTarget: BuildTarget): String =
    if (buildTarget.kind.ruleType == RuleType.TEST) GO_TEST_SOURCE_ROOT_TYPE else GO_SOURCE_ROOT_TYPE

  private fun getResourceContentRootEntities(
    target: RawBuildTarget,
    entitySource: BazelModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> =
    target.resources.map { resource ->
      val resourceUrl = resource.asDirectoryOrParent(virtualFileUrlManager)
      val resourceRootEntity =
        SourceRootEntity(
          url = resourceUrl,
          rootTypeId = SourceRootTypeId(GO_RESOURCE_ROOT_TYPE),
          entitySource = entitySource,
        )
      ContentRootEntity(
        url = resourceUrl,
        excludedPatterns = listOf(),
        entitySource = entitySource,
      ) {
        this.excludedUrls = listOf()
        this.sourceRoots = listOf(resourceRootEntity)
      }
    }

  private fun Path.asDirectoryOrParent(virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl {
    val url = this.toVirtualFileUrl(virtualFileUrlManager)

    return if (url.virtualFile?.isDirectory == true) url else url.parent ?: error("Could not find parent for $url.")
  }

  private suspend fun prepareVgoModule(
    environment: ProjectSyncHook.ProjectSyncHookEnvironment,
    inputEntity: RawBuildTarget,
    moduleId: ModuleId,
    virtualFileUrlManager: VirtualFileUrlManager,
    idToGoTargetMap: Map<Label, BuildTarget>,
    entitySource: BazelModuleEntitySource,
  ): VgoStandaloneModuleEntity.Builder {
    val goBuildInfo = extractGoBuildTarget(inputEntity)

    val vgoModuleDependencies =
      inputEntity.dependencies.mapNotNull {
        idToGoTargetMap[it]?.let { dependencyTargetInfo ->
          val goDependencyBuildInfo = extractGoBuildTarget(dependencyTargetInfo)
          goDependencyBuildInfo?.let { goDepBuildInfo ->
            VgoDependencyEntity(
              importPath = goDepBuildInfo.importPath,
              entitySource = entitySource,
              isMainModule = false,
              internal = true,
            ) {
              this.root = dependencyTargetInfo.baseDirectory.toVirtualFileUrl(virtualFileUrlManager)
            }
          }
        }
      }

    val vgoModuleLibraries =
      queryGoLibraries(environment).libraries.map {
        VgoDependencyEntity(
          importPath = it.goImportPath ?: "",
          entitySource = entitySource,
          isMainModule = false,
          internal = false,
        ) {
          this.root = it.goRoot?.toVirtualFileUrl(virtualFileUrlManager)
        }
      }

    return VgoStandaloneModuleEntity(
      moduleId = moduleId,
      entitySource = entitySource,
      importPath = goBuildInfo?.importPath ?: "",
      root = inputEntity.baseDirectory.toVirtualFileUrl(virtualFileUrlManager),
    ) {
      this.dependencies = vgoModuleDependencies + vgoModuleLibraries
    }
  }

  private suspend fun queryGoLibraries(environment: ProjectSyncHook.ProjectSyncHookEnvironment): WorkspaceGoLibrariesResult =
    coroutineScope {
      query("workspace/goLibraries") {
        environment.server.workspaceGoLibraries()
      }
    }

  private suspend fun calculateAndAddGoSdk(
    reporter: SequentialProgressReporter,
    goTargets: List<BuildTarget>,
    project: Project,
    taskId: String,
  ) = reporter.indeterminateStep(BazelPluginBundle.message("progress.bar.calculate.go.sdk.infos")) {
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = "calculate-and-add-bsp-fetched-go-sdk",
      message = BazelPluginBundle.message("console.task.model.calculate.add.go.fetched.sdk"),
    ) {
      goTargets
        .findGoSdkOrNull()
        ?.setAsUsed(project)
    }
  }

  private fun List<BuildTarget>.findGoSdkOrNull(): GoSdk? =
    firstNotNullOfOrNull { extractGoBuildTarget(it)?.sdkHomePath }
      ?.let { GoSdk.fromHomePath(it.toString()) }

  private suspend fun GoSdk.setAsUsed(project: Project) {
    val goSdkService = GoSdkService.getInstance(project)
    writeAction { goSdkService.setSdk(this) }
  }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1154
  private fun restoreGoModulesRegistry(project: Project) {
    VgoWorkspaceModelUpdater(project).restoreModulesRegistry()
  }

  private suspend fun enableGoSupportInTargets(
    moduleEntities: List<ModuleEntity>,
    project: Project,
    taskId: String,
  ) = project.syncConsole.withSubtask(
    taskId,
    "enable-go-support-in-targets",
    BazelPluginBundle.message("console.task.model.add.go.support.in.targets"),
  ) {
    val workspaceModel = WorkspaceModel.getInstance(project)
    moduleEntities.forEach { moduleEntity ->
      moduleEntity.findModule(workspaceModel.currentSnapshot)?.let { module ->
        writeAction {
          module.enableGoSupport()
        }
      }
    }
  }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1155
  private fun Module.enableGoSupport() {
    GoModuleSettings.getInstance(this).isGoSupportEnabled = true
  }
}
