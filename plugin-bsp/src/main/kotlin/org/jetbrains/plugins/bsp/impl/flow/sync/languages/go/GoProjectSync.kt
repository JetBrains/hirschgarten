package org.jetbrains.plugins.bsp.impl.flow.sync.languages.go

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
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
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import org.jetbrains.plugins.bsp.building.syncConsole
import org.jetbrains.plugins.bsp.building.withSubtask
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfo
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfos
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook
import org.jetbrains.plugins.bsp.impl.flow.sync.queryIf
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.RawUriToDirectoryPathTransformer
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspEntitySource
import kotlin.io.path.toPath

private const val GO_SOURCE_ROOT_TYPE = "go-source"
private const val GO_TEST_SOURCE_ROOT_TYPE = "go-test"
private const val GO_RESOURCE_ROOT_TYPE = "go-resource"

class GoProjectSync : ProjectSyncHook {
  override val buildToolId: BuildToolId = bspBuildToolId

  override fun isEnabled(project: Project): Boolean = BspFeatureFlags.isGoSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val goTargets = environment.baseTargetInfos.calculateGoTargets()
    val idToGoTargetMap = goTargets.associateBy({ it.target.id }, { it })
    val virtualFileUrlManager = WorkspaceModel.getInstance(environment.project).getVirtualFileUrlManager()

    val moduleEntities =
      goTargets.map {
        val moduleEntity =
          addModuleEntityFromTarget(
            builder = environment.diff.workspaceModelDiff.mutableEntityStorage,
            target = it,
            virtualFileUrlManager = virtualFileUrlManager,
          )
        val vgoModule = prepareVgoModule(environment, it, moduleEntity.symbolicId, virtualFileUrlManager, idToGoTargetMap)
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
    virtualFileUrlManager: VirtualFileUrlManager,
  ): ModuleEntity {
    val sourceContentRootEntities = getSourceContentRootEntities(target, virtualFileUrlManager)
    val resourceContentRootEntities = getResourceContentRootEntities(target, virtualFileUrlManager)
    return builder.addEntity(
      ModuleEntity(
        name = target.moduleName,
        dependencies =
          target.target.dependencies.map {
            ModuleDependency(
              module = ModuleId(it.uri),
              exported = true,
              scope = DependencyScope.COMPILE,
              productionOnTest = true,
            )
          },
        entitySource = BspEntitySource,
      ) {
        // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1153
        this.type = ModuleTypeId("WEB_MODULE")
        this.contentRoots = sourceContentRootEntities + resourceContentRootEntities
      },
    )
  }

  private val BaseTargetInfo.moduleName: String
    get() = this.target.displayName

  private fun getSourceContentRootEntities(
    target: BaseTargetInfo,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> =
    target.sources.flatMap {
      it.sources.map { source ->
        val sourceUrl = virtualFileUrlManager.getOrCreateFromUrl(source.uri)
        val sourceRootEntity =
          SourceRootEntity(
            url = sourceUrl,
            rootTypeId = SourceRootTypeId(inferRootType(target.target)),
            entitySource = BspEntitySource,
          )
        ContentRootEntity(
          url = sourceUrl,
          excludedPatterns = ArrayList(),
          entitySource = BspEntitySource,
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
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> =
    target.resources.flatMap {
      it.resources.map { resource ->
        val resourceUrl = RawUriToDirectoryPathTransformer.transform(resource).toVirtualFileUrl(virtualFileUrlManager)
        val resourceRootEntity =
          SourceRootEntity(
            url = resourceUrl,
            rootTypeId = SourceRootTypeId(GO_RESOURCE_ROOT_TYPE),
            entitySource = BspEntitySource,
          )
        ContentRootEntity(
          url = resourceUrl,
          excludedPatterns = listOf(),
          entitySource = BspEntitySource,
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
  ): VgoStandaloneModuleEntity.Builder {
    val goBuildInfo = extractGoBuildTarget(inputEntity.target)

    val vgoModuleDependencies =
      inputEntity.target.dependencies.mapNotNull {
        idToGoTargetMap[it]?.let { dependencyTargetInfo ->
          val goDependencyBuildInfo = extractGoBuildTarget(dependencyTargetInfo.target)
          goDependencyBuildInfo?.let { goDepBuildInfo ->
            VgoDependencyEntity(
              importPath = goDepBuildInfo.importPath,
              entitySource = BspEntitySource,
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
          entitySource = BspEntitySource,
          isMainModule = false,
          internal = false,
        ) {
          this.root = it.goRoot?.toPath()?.toVirtualFileUrl(virtualFileUrlManager)
        }
      }

    return VgoStandaloneModuleEntity(
      moduleId = moduleId,
      entitySource = BspEntitySource,
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
