package org.jetbrains.bazel.golang.sync

import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.vgo.project.workspaceModel.VgoWorkspaceModelUpdater
import com.goide.vgo.project.workspaceModel.entities.VgoDependencyEntity
import com.goide.vgo.project.workspaceModel.entities.VgoStandaloneModuleEntity
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
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
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModule
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.findNameProvider
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.RawUriToDirectoryPathTransformer
import org.jetbrains.bazel.magicmetamodel.orDefault
import org.jetbrains.bazel.sync.BaseTargetInfo
import org.jetbrains.bazel.sync.BaseTargetInfos
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.queryIf
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bazel.workspacemodel.entities.BspModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import kotlin.io.path.toPath

private const val GO_SOURCE_ROOT_TYPE = "go-source"
private const val GO_TEST_SOURCE_ROOT_TYPE = "go-test"
private const val GO_RESOURCE_ROOT_TYPE = "go-resource"

// looks strange, but GO plugin does not use this field extensively, so there is no GO-specific type available
// WEB_MODULE is the "recommended" one
private val GO_MODULE_TYPE = ModuleTypeId("WEB_MODULE")

class GoProjectSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BspFeatureFlags.isGoSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val goTargets = environment.baseTargetInfos.calculateGoTargets()
    val idToGoTargetMap = goTargets.associateBy({ it.target.id }, { it })
    val virtualFileUrlManager = WorkspaceModel.getInstance(environment.project).getVirtualFileUrlManager()
    val moduleNameProvider = environment.project.findNameProvider().orDefault()

    val moduleEntities =
      goTargets.map {
        val moduleName = moduleNameProvider(BuildTargetInfo(id = it.target.id))
        val moduleSourceEntity = BspModuleEntitySource(moduleName)

        val moduleEntity =
          addModuleEntityFromTarget(
            builder = environment.diff.workspaceModelDiff.mutableEntityStorage,
            target = it,
            moduleName = moduleName,
            entitySource = moduleSourceEntity,
            virtualFileUrlManager = virtualFileUrlManager,
            moduleNameProvider = moduleNameProvider,
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

  private fun BaseTargetInfos.calculateGoTargets(): List<BaseTargetInfo> = infos.filter { it.target.languageIds.contains("go") }

  private fun addModuleEntityFromTarget(
    builder: MutableEntityStorage,
    target: BaseTargetInfo,
    moduleName: String,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
    moduleNameProvider: TargetNameReformatProvider,
  ): ModuleEntity {
    val sourceContentRootEntities = getSourceContentRootEntities(target, entitySource, virtualFileUrlManager)
    val resourceContentRootEntities = getResourceContentRootEntities(target, entitySource, virtualFileUrlManager)
    return builder.addEntity(
      ModuleEntity(
        name = moduleName,
        dependencies =
          target.target.dependencies.map {
            ModuleDependency(
              module = ModuleId(moduleNameProvider(BuildTargetInfo(id = it))),
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
    target: BaseTargetInfo,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> =
    target.sources.flatMap {
      it.sources.map { source ->
        val sourceUrl = virtualFileUrlManager.getOrCreateFromUrl(source.uri)
        val sourceRootEntity =
          SourceRootEntity(
            url = sourceUrl,
            rootTypeId = SourceRootTypeId(inferRootType(target.target)),
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
    }

  private fun inferRootType(buildTarget: BuildTarget): String =
    if (buildTarget.tags.contains("test")) GO_TEST_SOURCE_ROOT_TYPE else GO_SOURCE_ROOT_TYPE

  private fun getResourceContentRootEntities(
    target: BaseTargetInfo,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> =
    target.resources.flatMap {
      it.resources.map { resource ->
        val resourceUrl = RawUriToDirectoryPathTransformer.transform(resource).toVirtualFileUrl(virtualFileUrlManager)
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
    }

  private suspend fun prepareVgoModule(
    environment: ProjectSyncHook.ProjectSyncHookEnvironment,
    inputEntity: BaseTargetInfo,
    moduleId: ModuleId,
    virtualFileUrlManager: VirtualFileUrlManager,
    idToGoTargetMap: Map<BuildTargetIdentifier, BaseTargetInfo>,
    entitySource: BspModuleEntitySource,
  ): VgoStandaloneModuleEntity.Builder {
    val goBuildInfo = extractGoBuildTarget(inputEntity.target)

    val vgoModuleDependencies =
      inputEntity.target.dependencies.mapNotNull {
        idToGoTargetMap[it]?.let { dependencyTargetInfo ->
          val goDependencyBuildInfo = extractGoBuildTarget(dependencyTargetInfo.target)
          goDependencyBuildInfo?.let { goDepBuildInfo ->
            VgoDependencyEntity(
              importPath = goDepBuildInfo.importPath,
              entitySource = entitySource,
              isMainModule = false,
              internal = true,
            ) {
              this.root = virtualFileUrlManager.getOrCreateFromUrl(dependencyTargetInfo.target.baseDirectory)
            }
          }
        }
      }

    val vgoModuleLibraries =
      queryGoLibraries(environment)?.libraries?.map {
        VgoDependencyEntity(
          importPath = it.goImportPath ?: "",
          entitySource = entitySource,
          isMainModule = false,
          internal = false,
        ) {
          this.root = it.goRoot?.toPath()?.toVirtualFileUrl(virtualFileUrlManager)
        }
      }

    return VgoStandaloneModuleEntity(
      moduleId = moduleId,
      entitySource = entitySource,
      importPath = goBuildInfo?.importPath ?: "",
      root = virtualFileUrlManager.getOrCreateFromUrl(inputEntity.target.baseDirectory),
    ) {
      this.dependencies = vgoModuleDependencies + (vgoModuleLibraries ?: listOf())
    }
  }

  private suspend fun queryGoLibraries(environment: ProjectSyncHook.ProjectSyncHookEnvironment): WorkspaceGoLibrariesResult? =
    coroutineScope {
      queryIf(environment.capabilities.workspaceLibrariesProvider, "workspace/goLibraries") {
        environment.server.workspaceGoLibraries()
      }
    }

  private suspend fun calculateAndAddGoSdk(
    reporter: SequentialProgressReporter,
    goTargets: List<BaseTargetInfo>,
    project: Project,
    taskId: String,
  ) = reporter.indeterminateStep(BspPluginBundle.message("progress.bar.calculate.go.sdk.infos")) {
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = "calculate-and-add-bsp-fetched-go-sdk",
      message = BspPluginBundle.message("console.task.model.calculate.add.go.fetched.sdk"),
    ) {
      goTargets
        .findGoSdkOrNull()
        ?.setAsUsed(project)
    }
  }

  private fun List<BaseTargetInfo>.findGoSdkOrNull(): GoSdk? =
    firstNotNullOfOrNull { extractGoBuildTarget(it.target)?.sdkHomePath }
      ?.let { GoSdk.fromHomePath(it.path) }

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
    BspPluginBundle.message("console.task.model.add.go.support.in.targets"),
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
