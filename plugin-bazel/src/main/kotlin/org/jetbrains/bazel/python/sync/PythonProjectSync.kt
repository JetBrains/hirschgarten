package org.jetbrains.bazel.python.sync

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
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
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater
import com.jetbrains.python.sdk.detectSystemWideSdks
import com.jetbrains.python.sdk.guessedLanguageLevel
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.findNameProvider
import org.jetbrains.bazel.magicmetamodel.orDefault
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bazel.workspacemodel.entities.BspModuleEntitySource
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import java.nio.file.Path
import kotlin.io.path.Path

private const val PYTHON_SDK_ID = "PythonSDK"
private const val PYTHON_SOURCE_ROOT_TYPE = "python-source"
private const val PYTHON_RESOURCE_ROOT_TYPE = "python-resource"
private val PYTHON_MODULE_TYPE = ModuleTypeId("PYTHON_MODULE")

class PythonProjectSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.isPythonSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val pythonTargets = environment.buildTargets.calculatePythonTargets()
    val moduleNameProvider = environment.project.findNameProvider().orDefault()
    val virtualFileUrlManager = WorkspaceModel.getInstance(environment.project).getVirtualFileUrlManager()
    val sdks = calculateAndAddSdks(pythonTargets, environment, virtualFileUrlManager)

    sdks.values.updateAll(environment.project)

    pythonTargets.forEach {
      val moduleName = moduleNameProvider(it.id)
      val moduleSourceEntity = BspModuleEntitySource(moduleName)

      addModuleEntityFromTarget(
        builder = environment.diff.workspaceModelDiff.mutableEntityStorage,
        target = it,
        moduleName = moduleName,
        entitySource = moduleSourceEntity,
        virtualFileUrlManager = virtualFileUrlManager,
        moduleNameProvider = moduleNameProvider,
        sdks[it.id],
      )
    }
  }

  private fun WorkspaceBuildTargetsResult.calculatePythonTargets(): List<BuildTarget> = targets.filter { it.languageIds.contains("python") }

  private fun addModuleEntityFromTarget(
    builder: MutableEntityStorage,
    target: BuildTarget,
    moduleName: String,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
    moduleNameProvider: TargetNameReformatProvider,
    sdk: Sdk?,
  ): ModuleEntity {
    val contentRoots = getContentRootEntities(target, entitySource, virtualFileUrlManager)

    val dependencies =
      target.dependencies.map {
        ModuleDependency(
          module = ModuleId(moduleNameProvider(it)),
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
    target: BuildTarget,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> {
    val sourceContentRootEntities = getSourceContentRootEntities(target, entitySource, virtualFileUrlManager)
    val resourceContentRootEntities = getResourceContentRootEntities(target, entitySource, virtualFileUrlManager)

    return sourceContentRootEntities + resourceContentRootEntities
  }

  private fun getSourceContentRootEntities(
    target: BuildTarget,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> =
    target.sources.map { source ->
      val sourceUrl = source.path.toVirtualFileUrl(virtualFileUrlManager)
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

  private fun getResourceContentRootEntities(
    target: BuildTarget,
    entitySource: BspModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> =
    target.resources.map { resource ->
      val resourceUrl = resource.toVirtualFileUrl(virtualFileUrlManager)
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

  private suspend fun calculateAndAddSdks(
    targets: List<BuildTarget>,
    environment: ProjectSyncHookEnvironment,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): Map<Label, Sdk> =
    environment.progressReporter.indeterminateStep(text = BazelPluginBundle.message("progress.bar.calculate.python.sdk.infos")) {
      environment.project.syncConsole.withSubtask(
        taskId = environment.taskId,
        subtaskId = "calculate-and-add-all-python-sdk-infos",
        message = BazelPluginBundle.message("console.task.model.calculate.python.sdks"),
      ) {
        doCalculateAndAddSdks(calculateDependenciesSources(targets, environment), targets, virtualFileUrlManager)
      }
    }

  private suspend fun doCalculateAndAddSdks(
    targetIdToDependenciesSourcesMap: Map<String, List<DependencySourcesItem>>,
    targets: List<BuildTarget>,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): Map<Label, Sdk> {
    val detectedSdk: PyDetectedSdk? = getSystemSdk()
    return targets
      .mapNotNull { targetInfo ->
        val sdk =
          calculateAndAddSdkIfPossible(
            target = targetInfo,
            dependenciesSources = targetIdToDependenciesSourcesMap[targetInfo.id.toShortString()] ?: emptyList(),
            defaultSdk = detectedSdk,
            virtualFileUrlManager = virtualFileUrlManager,
          ) ?: return@mapNotNull null

        targetInfo.id to sdk
      }.toMap()
  }

  private suspend fun calculateDependenciesSources(
    targets: List<BuildTarget>,
    environment: ProjectSyncHookEnvironment,
  ): Map<String, List<DependencySourcesItem>> = queryDependenciesSources(environment, targets).items.groupBy { it.target.toShortString() }

  private suspend fun queryDependenciesSources(
    environment: ProjectSyncHookEnvironment,
    targets: List<BuildTarget>,
  ): DependencySourcesResult =
    query("buildTarget/dependencySources") {
      environment.server.buildTargetDependencySources(DependencySourcesParams(targets.map { it.id }))
    }

  private suspend fun calculateAndAddSdkIfPossible(
    target: BuildTarget,
    dependenciesSources: List<DependencySourcesItem>,
    defaultSdk: PyDetectedSdk?,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): Sdk? =
    extractPythonBuildTarget(target)?.let {
      if (it.interpreter != null && it.version != null) {
        calculateAndAddSdk(
          sdkName = "${target.id.toShortString()}-${it.version}",
          sdkInterpreterUri = it.interpreter!!,
          sdkDependencies = dependenciesSources,
          virtualFileUrlManager = virtualFileUrlManager,
        )
      } else {
        defaultSdk
          ?.homePath
          ?.let { homePath ->
            calculateAndAddSdk(
              sdkName = "${target.id.toShortString()}-detected",
              sdkInterpreterUri = Path(homePath),
              sdkDependencies = dependenciesSources,
              virtualFileUrlManager = virtualFileUrlManager,
            )
          }
      }
    }

  private suspend fun calculateAndAddSdk(
    sdkName: String,
    sdkInterpreterUri: Path,
    sdkDependencies: List<DependencySourcesItem>,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): Sdk {
    val sdkTable = ProjectJdkTable.getInstance()
    val existingSdk = sdkTable.findJdk(sdkName, PythonSdkType.getInstance().toString())

    val sdk =
      existingSdk ?: ProjectJdkImpl(
        sdkName,
        PythonSdkType.getInstance(),
      )

    val modificator = sdk.sdkModificator
    modificator.homePath =
      sdkInterpreterUri.toString()
    modificator.versionString // needs to be invoked in order to fetch the version and cache it

    val additionalData = PythonSdkAdditionalData()
    val virtualFiles =
      sdkDependencies
        .flatMap { it.sources }
        .mapNotNull {
          it
            .toVirtualFileUrl(virtualFileUrlManager)
            .virtualFile
        }.toSet()
    additionalData.setAddedPathsFromVirtualFiles(virtualFiles)
    modificator.sdkAdditionalData = additionalData

    writeAction {
      modificator.commitChanges()
      if (existingSdk == null) {
        sdkTable.addJdk(sdk)
      }
    }
    return sdk
  }

  private fun getSystemSdk(): PyDetectedSdk? =
    detectSystemWideSdks(null, emptyList())
      .filter { it.homePath != null }
      .let { sdks ->
        sdks.firstOrNull { it.guessedLanguageLevel?.isPy3K == true } ?: sdks.firstOrNull()
      }

  private fun Collection<Sdk>.updateAll(project: Project) =
    forEach {
      PythonSdkUpdater.scheduleUpdate(it, project)
    }
}
