package com.intellij.bazel.python.backend

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ContentRootEntityBuilder
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryEntityBuilder
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
import com.jetbrains.python.PythonBinary
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater
import com.jetbrains.python.sdk.detectSystemWideSdks
import com.jetbrains.python.sdk.guessedLanguageLevel
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.languages.python.PythonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.allTargets
import org.jetbrains.bazel.sync.workspace.snapshot.filterBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.allSources
import org.jetbrains.bsp.protocol.utils.StringUtils
import java.nio.file.Path
import kotlin.io.path.pathString

private const val PYTHON_SDK_ID = "PythonSDK"
private const val PYTHON_SOURCE_ROOT_TYPE = "python-source"
private const val PYTHON_RESOURCE_ROOT_TYPE = "python-resource"
private val PYTHON_MODULE_TYPE = ModuleTypeId("PYTHON_MODULE")

internal class BazelPythonWorkspaceImporter : BazelWorkspaceImporter {
  companion object {
    internal val logger = logger<BazelPythonWorkspaceImporter>()
  }

  private lateinit var commonSyncConfig: CommonWorkspaceSyncConfig
  private lateinit var pythonSyncConfig: PythonWorkspaceSyncConfig

  private var defaultInterpreter: Path? = null
  private var defaultVersion: String? = null
  private var pySourceDeps: Map<WorkspaceTargetKey, List<Path>> = mapOf()
  private var pySdks: Map<WorkspaceTargetKey, Sdk?> = mapOf()
  private var pyDefaultSdk: Sdk? = null

  override suspend fun import(
    context: WorkspaceImporterContext,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> = runCatching {
    when (phase) {
      WorkspaceImporterPhase.Initialize -> onInitialize(context, snapshot)
      is WorkspaceImporterPhase.WorkspaceApply -> onWorkspaceApply(context, snapshot, phase.builder, context.vfuManager)
      WorkspaceImporterPhase.PostProcessing -> onPostProcessing(context, snapshot)
      else -> WorkspaceImporterResult.Success
    }
  }

  private suspend fun onInitialize(context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): WorkspaceImporterResult {
    commonSyncConfig = snapshot.syncConfigs.filterIsInstance<CommonWorkspaceSyncConfig>()
                         .firstOrNull() ?: throw IllegalStateException()
    pythonSyncConfig = snapshot.syncConfigs.filterIsInstance<PythonWorkspaceSyncConfig>()
                         .firstOrNull() ?: throw IllegalStateException()

    if (!pythonSyncConfig.isPythonSupportEnabled) {
      return WorkspaceImporterResult.Abort
    }

    val defaultPythonTarget = snapshot.allTargets
      .filterBuildTarget<PythonBuildTarget>()
      .filter { (_, pythonTarget) -> pythonTarget.interpreter != null }
      .map { (_, pythonTarget) -> pythonTarget }
      .firstOrNull()
    defaultInterpreter = defaultPythonTarget?.interpreter
    defaultVersion = defaultPythonTarget?.version

    // MAYBE RC: obvious incremental caching candidate...
    pySourceDeps = snapshot.allTargets.filterBuildTarget<PythonBuildTarget>()
      .associate { (target, _) ->
        target.targetKey to snapshot.targetGraph.findAllTransitiveSuccessorsWithoutRootTargets(target.targetKey)
          .filterBuildTarget<PythonBuildTarget>()
          .flatMap { (_, pythonTarget) -> pythonTarget.externalSources?.getFiles() ?: sequenceOf() }
          .toList()
      }

    pySdks = calculateAndAddSdksWithProgress(context, snapshot)
    pyDefaultSdk = getSystemSdk()

    return WorkspaceImporterResult.Success
  }

  private suspend fun onWorkspaceApply(
    context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot,
    builder: MutableEntityStorage, vfuManager: VirtualFileUrlManager,
  ): WorkspaceImporterResult {
    for ((target, pyTarget) in snapshot.allTargets.filterBuildTarget<PythonBuildTarget>()) {
      val targetKey = target.targetKey
      val moduleName = targetKey.label.formatAsModuleName(snapshot.repoMapping)
      val moduleSourceEntity = BazelModuleEntitySource(moduleName)
      val sourceDeps = pySourceDeps[target.targetKey] ?: emptyList()
      val sourceDependencyLibrary =
        calculateSourceDependencyLibrary(targetKey.label, sourceDeps, moduleSourceEntity, vfuManager)

      addModuleEntityFromTarget(
        builder = builder,
        repoMapping = snapshot.repoMapping,
        target = target.rawBuildTarget,
        moduleName = moduleName,
        entitySource = moduleSourceEntity,
        virtualFileUrlManager = vfuManager,
        sdk = pySdks[targetKey] ?: pyDefaultSdk,
        sourceDependencyLibrary = sourceDependencyLibrary,
      )
    }
    return WorkspaceImporterResult.Success
  }

  private suspend fun onPostProcessing(context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): WorkspaceImporterResult {
    val pyTargets = snapshot.allTargets
      .filter { it.hasBuildData<PythonBuildTarget>() }
      .map { it.rawBuildTarget }
      .toList()
    context.project.connection.runWithServer { server ->
      context.project.serviceAsync<PythonResolveIndexService>()
        .updatePythonResolveIndex(pyTargets, server.outFileHardLinks)
    }
    return WorkspaceImporterResult.Success
  }

  private suspend fun calculateAndAddSdksWithProgress(
    context: WorkspaceImporterContext,
    snapshot: WorkspaceSnapshot,
  ): Map<WorkspaceTargetKey, Sdk?> =
    context.progressReporter.indeterminateStep(text = BazelPythonBackendBundle.message("progress.bar.calculate.python.sdk.infos")) {
      context.taskConsole.withSubtask(
        subtaskId = context.taskId.subTask("calculate-and-add-all-python-sdk-infos"),
        message = BazelPythonBackendBundle.message("console.task.model.calculate.python.sdks"),
      ) {
        calculateAndAddSdks(context, snapshot)
      }
    }

  // MAYBE RC: we probably should postpone sdk table modification
  //   to post processing step for now its fine
  private suspend fun calculateAndAddSdks(
    context: WorkspaceImporterContext,
    snapshot: WorkspaceSnapshot,
  ): Map<WorkspaceTargetKey, Sdk?> {
    return snapshot.allTargets.filterBuildTarget<PythonBuildTarget>()
      .filter { (_, pyTarget) -> (pyTarget.interpreter) != null }
      .associateBy { (target, _) -> target.targetKey }
      .mapValues { (_, value) -> findOrAddSdk(value.second, context.project) }
  }

  private fun calculateSourceDependencyLibrary(
    target: Label,
    sourceDependencies: List<Path>,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): LibraryEntityBuilder? {
    val roots =
      sourceDependencies.distinct().map {
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
    }
    else {
      null
    }
  }

  private fun addModuleEntityFromTarget(
    builder: MutableEntityStorage,
    repoMapping: RepoMapping,
    target: RawBuildTarget,
    moduleName: String,
    entitySource: BazelModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
    sdk: Sdk?,
    sourceDependencyLibrary: LibraryEntityBuilder? = null,
  ): ModuleEntity {
    val contentRoots = getContentRootEntities(target, entitySource, virtualFileUrlManager)

    val libraryDependency =
      sourceDependencyLibrary?.let {
        val addedLibrary = builder.addEntity(it)
        LibraryDependency(addedLibrary.symbolicId, true, DependencyScope.COMPILE)
      }

    val dependencies =
      target.dependencies.map {
        ModuleDependency(
          module = ModuleId(it.label.formatAsModuleName(repoMapping)),
          exported = true,
          scope = DependencyScope.COMPILE,  // Python does not have the runtime/compile scope separation
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
  ): List<ContentRootEntityBuilder> {
    val sourceContentRootEntities = getSourceContentRootEntities(target as RawBuildTarget, entitySource, virtualFileUrlManager)
    val resourceContentRootEntities = getResourceContentRootEntities(target, entitySource, virtualFileUrlManager)

    return sourceContentRootEntities + resourceContentRootEntities
  }

  private fun getSourceContentRootEntities(
    target: RawBuildTarget,
    entitySource: BazelModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntityBuilder> =
    target.allSources.map { source ->
      val sourceUrl = source.toVirtualFileUrl(virtualFileUrlManager)
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
    }.toList()

  private fun getResourceContentRootEntities(
    target: RawBuildTarget,
    entitySource: BazelModuleEntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntityBuilder> =
    target.resources.getFiles().map { resource ->
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
    }.toList()


  private suspend fun findOrAddSdk(pythonTarget: PythonBuildTarget, project: Project): Sdk {
    val interpreter = pythonTarget.interpreter
                      ?: defaultInterpreter
                      ?: error("interpreter not found")
    val sdkName = chooseSdkName(interpreter, project.name)
    val sdkTable = ProjectJdkTable.getInstance()

    val existingSdk = sdkTable.findJdk(sdkName, PythonSdkType.getInstance().toString())
    if (existingSdk != null) return existingSdk

    val sdk =
      ProjectJdkImpl(
        sdkName,
        PythonSdkType.getInstance(),
        pythonTarget.interpreter.toString(),
        pythonTarget.version,
      )

    return sdk.also { addSdkToTable(it, project) }
  }

  private suspend fun addSdkToTable(sdk: Sdk, project: Project): Sdk {
    writeAction {
      ProjectJdkTable.getInstance().addJdk(sdk)
      PythonSdkUpdater.scheduleUpdate(sdk, project)
    }
    return sdk
  }
}

@ApiStatus.Internal
@VisibleForTesting
fun chooseSdkName(interpreter: PythonBinary, projectName: String): String =
  "$projectName-python-${StringUtils.md5Hash(interpreter.pathString, 5)}"

private fun getSystemSdk(): PyDetectedSdk? =
  detectSystemWideSdks(null, emptyList())
    .filter { it.homePath != null }
    .let { sdks ->
      sdks.firstOrNull { it.guessedLanguageLevel?.isPy3K == true } ?: sdks.firstOrNull()
    }
