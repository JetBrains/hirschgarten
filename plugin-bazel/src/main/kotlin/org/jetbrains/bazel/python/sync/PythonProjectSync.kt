package org.jetbrains.bazel.python.sync

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.findNameProvider
import org.jetbrains.bazel.magicmetamodel.orDefault
import org.jetbrains.bazel.sync.BaseTargetInfo
import org.jetbrains.bazel.sync.BaseTargetInfos
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.workspacemodel.entities.BspModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

private const val PYTHON_SDK_ID = "PythonSDK"
private const val PYTHON_SOURCE_ROOT_TYPE = "python-source"
private const val PYTHON_RESOURCE_ROOT_TYPE = "python-resource"
private val PYTHON_MODULE_TYPE = ModuleTypeId("PYTHON_MODULE")

class PythonProjectSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BspFeatureFlags.isPythonSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val pythonTargets = environment.baseTargetInfos.calculatePythonTargets()
    val moduleNameProvider = environment.project.findNameProvider().orDefault()
    val virtualFileUrlManager = WorkspaceModel.getInstance(environment.project).getVirtualFileUrlManager()

    pythonTargets.forEach {
      val moduleName = moduleNameProvider(BuildTargetInfo(id = it.target.id))
      val moduleSourceEntity = BspModuleEntitySource(moduleName)

      addModuleEntityFromTarget(
        builder = environment.diff.workspaceModelDiff.mutableEntityStorage,
        target = it,
        moduleName = moduleName,
        entitySource = moduleSourceEntity,
        virtualFileUrlManager = virtualFileUrlManager,
        moduleNameProvider = moduleNameProvider,
        null,
      )
    }
  }

  private fun BaseTargetInfos.calculatePythonTargets(): List<BaseTargetInfo> = infos.filter { it.target.languageIds.contains("python") }

  private fun addModuleEntityFromTarget(
    builder: MutableEntityStorage,
    target: BaseTargetInfo,
    moduleName: String,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
    moduleNameProvider: TargetNameReformatProvider,
    sdk: Sdk?,
  ): ModuleEntity {
    val contentRoots = getContentRootEntities(target, entitySource, virtualFileUrlManager)

    val dependencies =
      target.target.dependencies.map {
        ModuleDependency(
          module = ModuleId(moduleNameProvider(BuildTargetInfo(id = it))),
          exported = true,
          scope = DependencyScope.COMPILE,
          productionOnTest = true,
        )
      }

    val allDependencies = sdk?.toModuleDependencyItem()?.let { dependencies + it } ?: dependencies

    return builder.addEntity(
      ModuleEntity(
        name = moduleName,
        dependencies = allDependencies,
        entitySource = entitySource,
      ) {
        this.type = PYTHON_MODULE_TYPE
        this.contentRoots = contentRoots
      },
    )
  }

  private fun Sdk.toModuleDependencyItem(): ModuleDependencyItem = SdkDependency(SdkId(name, PYTHON_SDK_ID))

  private fun getContentRootEntities(
    target: BaseTargetInfo,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> {
    val sourceContentRootEntities = getSourceContentRootEntities(target, entitySource, virtualFileUrlManager)
    val resourceContentRootEntities = getResourceContentRootEntities(target, entitySource, virtualFileUrlManager)

    return sourceContentRootEntities + resourceContentRootEntities
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
            rootTypeId = SourceRootTypeId(PYTHON_SOURCE_ROOT_TYPE),
            entitySource = entitySource,
          )
        ContentRootEntity(
          url = sourceUrl,
          excludedPatterns = emptyList(),
          entitySource = entitySource,
        ) {
          this.excludedUrls = emptyList()
          this.sourceRoots = listOf(sourceRootEntity)
        }
      }
    }

  private fun getResourceContentRootEntities(
    target: BaseTargetInfo,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> =
    target.resources.flatMap {
      it.resources.map { resource ->
        val resourceUrl = virtualFileUrlManager.getOrCreateFromUrl(resource)
        val resourceRootEntity =
          SourceRootEntity(
            url = resourceUrl,
            rootTypeId = SourceRootTypeId(PYTHON_RESOURCE_ROOT_TYPE),
            entitySource = entitySource,
          )
        ContentRootEntity(
          url = resourceUrl,
          excludedPatterns = emptyList(),
          entitySource = entitySource,
        ) {
          this.excludedUrls = emptyList()
          this.sourceRoots = listOf(resourceRootEntity)
        }
      }
    }
}
