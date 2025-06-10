package org.jetbrains.bazel.python.sync

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater
import com.jetbrains.python.sdk.detectSystemWideSdks
import com.jetbrains.python.sdk.guessedLanguageLevel
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.python.resolve.PythonResolveIndexService
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bazel.utils.StringUtils
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget

private const val PYTHON_SDK_ID = "PythonSDK"
private const val PYTHON_SOURCE_ROOT_TYPE = "python-source"
private const val PYTHON_RESOURCE_ROOT_TYPE = "python-resource"
private val PYTHON_MODULE_TYPE = ModuleTypeId("PYTHON_MODULE")

class PythonProjectSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.isPythonSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Process Python targets") {
      // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1960
      val bspBuildTargets = environment.server.workspaceBuildTargets()
      val pythonTargets = bspBuildTargets.calculatePythonTargets()
      val virtualFileUrlManager = environment.project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

      val sdks = calculateAndAddSdksWithProgress(pythonTargets, environment)
      val sourceDependencies = calculateDependenciesSources(pythonTargets.map { it.id }, environment)
      val defaultSdk = getSystemSdk()

      pythonTargets.forEach {
        val moduleName = it.id.formatAsModuleName(environment.project)
        val moduleSourceEntity = BazelModuleEntitySource(moduleName)
        val targetSourceDependencies = sourceDependencies[it.id] ?: emptyList()
        val sourceDependencyLibrary =
          calculateSourceDependencyLibrary(it.id, targetSourceDependencies, moduleSourceEntity, virtualFileUrlManager)

        addModuleEntityFromTarget(
          builder = environment.diff.workspaceModelDiff.mutableEntityStorage,
          target = it as RawBuildTarget,
          moduleName = moduleName,
          entitySource = moduleSourceEntity,
          virtualFileUrlManager = virtualFileUrlManager,
          project = environment.project,
          sdk = sdks[it.id] ?: defaultSdk,
          sourceDependencyLibrary = sourceDependencyLibrary,
        )
      }
      environment.project.service<PythonResolveIndexService>().updatePythonResolveIndex(bspBuildTargets.targets)
    }
  }

  private fun WorkspaceBuildTargetsResult.calculatePythonTargets(): List<BuildTarget> =
    targets.filter {
      it.kind.languageClasses.contains(LanguageClass.PYTHON)
    }

  private suspend fun calculateAndAddSdksWithProgress(
    targets: List<BuildTarget>,
    environment: ProjectSyncHookEnvironment,
  ): Map<Label, Sdk?> =
    environment.progressReporter.indeterminateStep(text = BazelPluginBundle.message("progress.bar.calculate.python.sdk.infos")) {
      environment.project.syncConsole.withSubtask(
        taskId = environment.taskId,
        subtaskId = "calculate-and-add-all-python-sdk-infos",
        message = BazelPluginBundle.message("console.task.model.calculate.python.sdks"),
      ) {
        calculateAndAddSdks(targets, environment.project)
      }
    }

  private suspend fun calculateAndAddSdks(targets: List<BuildTarget>, project: Project): Map<Label, Sdk?> {
    val interpretersByTarget =
      targets
        .associateWith { extractPythonBuildTarget(it) }
        .mapKeys { it.key.id }
    val sdksByInterpreter =
      interpretersByTarget.values
        .filterNotNull()
        .filter { it.interpreter != null }
        .distinct()
        .associateWith { findOrAddSdk(it, project) }

    return interpretersByTarget.mapValues { sdksByInterpreter[it.value] }
  }

  private suspend fun findOrAddSdk(interpreter: PythonBuildTarget, project: Project): Sdk {
    val sdkName = chooseSdkName(interpreter, project.name)
    val sdkTable = ProjectJdkTable.getInstance()

    val existingSdk = sdkTable.findJdk(sdkName, PythonSdkType.getInstance().toString())
    if (existingSdk != null) return existingSdk

    val sdk =
      ProjectJdkImpl(
        sdkName,
        PythonSdkType.getInstance(),
        interpreter.interpreter.toString(),
        interpreter.version,
      )

    writeAction {
      sdkTable.addJdk(sdk)
      PythonSdkUpdater.scheduleUpdate(sdk, project)
    }
    return sdk
  }

  private fun chooseSdkName(interpreter: PythonBuildTarget, projectName: String): String =
    "$projectName-python-${StringUtils.md5Hash(interpreter.interpreter.toString(), 5)}"

  private suspend fun calculateDependenciesSources(
    targets: List<Label>,
    environment: ProjectSyncHookEnvironment,
  ): Map<Label, List<DependencySourcesItem>> =
    environment.progressReporter.indeterminateStep(text = BazelPluginBundle.message("progress.bar.calculate.python.source.deps")) {
      environment.project.syncConsole.withSubtask(
        taskId = environment.taskId,
        subtaskId = "calculate-python-dependency-sources",
        message = BazelPluginBundle.message("console.task.model.calculate.python.source.deps"),
      ) {
        queryDependenciesSources(environment, targets).items.groupBy { it.target }
      }
    }

  private suspend fun queryDependenciesSources(environment: ProjectSyncHookEnvironment, targets: List<Label>): DependencySourcesResult =
    query("buildTarget/dependencySources") {
      environment.server.buildTargetDependencySources(DependencySourcesParams(targets))
    }

  private fun calculateSourceDependencyLibrary(
    target: Label,
    sourceDependencies: List<DependencySourcesItem>,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): LibraryEntity.Builder? {
    val roots =
      sourceDependencies.flatMap { it.sources }.distinct().map {
        LibraryRoot(
          url = it.toVirtualFileUrl(virtualFileUrlManager),
          type = LibraryRootTypeId.SOURCES,
        )
      }
    return if (roots.isNotEmpty()) {
      LibraryEntity(
        name = target.toString(),
        tableId = LibraryTableId.ProjectLibraryTableId,
        roots = roots,
        entitySource = entitySource,
      )
    } else {
      null
    }
  }

  private fun addModuleEntityFromTarget(
    builder: MutableEntityStorage,
    target: RawBuildTarget,
    moduleName: String,
    entitySource: BazelModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
    project: Project,
    sdk: Sdk?,
    sourceDependencyLibrary: LibraryEntity.Builder? = null,
  ): ModuleEntity {
    val contentRoots = getContentRootEntities(target, entitySource, virtualFileUrlManager)

    val libraryDependency =
      sourceDependencyLibrary?.let {
        val addedLibrary = builder.addEntity(it)
        LibraryDependency(addedLibrary.symbolicId, false, DependencyScope.COMPILE)
      }

    val dependencies =
      target.dependencies.map {
        ModuleDependency(
          module = ModuleId(it.formatAsModuleName(project)),
          exported = true,
          scope = DependencyScope.COMPILE,
          productionOnTest = true,
        )
      }

    val allDependencies = dependencies + listOfNotNull(sdk?.toModuleDependencyItem(), libraryDependency)

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
    entitySource: BazelModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntity.Builder> {
    val sourceContentRootEntities = getSourceContentRootEntities(target as RawBuildTarget, entitySource, virtualFileUrlManager)
    val resourceContentRootEntities = getResourceContentRootEntities(target, entitySource, virtualFileUrlManager)

    return sourceContentRootEntities + resourceContentRootEntities
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
    target: RawBuildTarget,
    entitySource: BazelModuleEntitySource,
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
}

private fun getSystemSdk(): PyDetectedSdk? =
  detectSystemWideSdks(null, emptyList())
    .filter { it.homePath != null }
    .let { sdks ->
      sdks.firstOrNull { it.guessedLanguageLevel?.isPy3K == true } ?: sdks.firstOrNull()
    }
