package com.intellij.bazel.python.backend

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
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
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.python.community.services.systemPython.SystemPythonService
import com.jetbrains.python.PythonBinary
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.sdk.ModuleOrProject.ProjectOnly
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.createLocalSdkGuessingTypeByPath
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import com.intellij.bazel.python.backend.sync.PythonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.allTargets
import org.jetbrains.bazel.sync.workspace.snapshot.filterBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleExtensionEntity
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabel
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabelList
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetSourceRootTypeId
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.allSources
import org.jetbrains.bsp.protocol.utils.StringUtils
import java.nio.file.Path
import kotlin.io.path.isDirectory
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
  private lateinit var moduleNameByKey: Map<WorkspaceTargetKey, String>

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
      is WorkspaceImporterPhase.WorkspaceApply -> onWorkspaceApply(context, snapshot, phase.builder, context.vfuManager, phase.entitySource)
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

    moduleNameByKey = snapshot.allTargets
      .filter { it.hasBuildData<PythonBuildTarget>() }
      .groupBy { it.targetKey.label }
      .flatMap { (_, targets) ->
        when {
          targets.size == 1 -> {
            val key = targets.single().targetKey
            listOf(key to key.formatAsModuleName(snapshot.repoMapping, withConfiguration = false))
          }

          else -> targets.map { it.targetKey to it.targetKey.formatAsModuleName(snapshot.repoMapping, withConfiguration = true) }
        }
      }
      .toMap()

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
    pyDefaultSdk = getSystemSdk(context.taskId, context.project, context.taskConsole)

    return WorkspaceImporterResult.Success
  }

  private fun onWorkspaceApply(
    context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot,
    builder: MutableEntityStorage, vfuManager: VirtualFileUrlManager,
    entitySource: EntitySource,
  ): WorkspaceImporterResult {
    for ((target, _) in snapshot.allTargets.filterBuildTarget<PythonBuildTarget>()) {
      val targetKey = target.targetKey
      val sourceDeps = pySourceDeps[target.targetKey] ?: emptyList()
      val sourceDependencyLibrary =
        calculateSourceDependencyLibrary(targetKey, snapshot.repoMapping, sourceDeps, entitySource, vfuManager)

      addModuleEntityFromTarget(
        context = context,
        builder = builder,
        repoMapping = snapshot.repoMapping,
        target = target.rawBuildTarget,
        moduleName = targetKey.toPythonModuleName(snapshot.repoMapping),
        entitySource = entitySource,
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

  private suspend fun findOrAddSdk(pythonTarget: PythonBuildTarget, project: Project): Sdk? {
    // Before this change we had toString() here so in case of null there would be "null"
    // This !! fails fast
    // TODO: Make make interpreter non-null
    val interpreter = pythonTarget.interpreter
                      ?: defaultInterpreter
                      ?: return null
    val sdkName = chooseSdkName(interpreter, project.name)
    val sdkTable = ProjectJdkTable.getInstance()

    val existingSdk = sdkTable.findJdk(sdkName, PythonSdkType.getInstance().toString())
    if (existingSdk != null) return existingSdk

    return when (val r = createSdkFromPython(interpreter, project, sdkName)) {
      is com.jetbrains.python.Result.Failure -> {
        logger.warn("Failed to create SDK for $interpreter: ${r.error}")
        null
      }

      is com.jetbrains.python.Result.Success -> r.result
    }
  }

  private fun calculateSourceDependencyLibrary(
    target: WorkspaceTargetKey,
    repoMapping: RepoMapping,
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
        name = target.toPythonModuleName(repoMapping),
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
    context: WorkspaceImporterContext,
    builder: MutableEntityStorage,
    repoMapping: RepoMapping,
    target: RawBuildTarget,
    moduleName: String,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
    sdk: Sdk?,
    sourceDependencyLibrary: LibraryEntityBuilder? = null,
  ): ModuleEntity {
    val contentRoots = getContentRootEntities(context, target, entitySource, virtualFileUrlManager)

    val libraryDependency =
      sourceDependencyLibrary?.let {
        val addedLibrary = builder.addEntity(it)
        LibraryDependency(addedLibrary.symbolicId, true, DependencyScope.COMPILE)
      }

    val dependencies =
      target.dependencies.map {
        ModuleDependency(
          module = ModuleId(it.targetKey.toPythonModuleName(repoMapping)),
          exported = true,
          scope = DependencyScope.COMPILE,  // Python does not have the runtime/compile scope separation
          productionOnTest = true,
        )
      }

    val allDependencies = dependencies + listOfNotNull(ModuleSourceDependency, sdk?.toModuleDependencyItem(), libraryDependency)

    return builder.addEntity(
      ModuleEntity(
        name = moduleName,
        dependencies = allDependencies,
        entitySource = entitySource,
      ) {
        this.type = PYTHON_MODULE_TYPE
        this.contentRoots = contentRoots
        this.bazelModuleExtension = BazelModuleExtensionEntity(
          label = WorkspaceModelTargetLabel(target.id),
          rootTypeId = WorkspaceModelTargetSourceRootTypeId(SourceRootTypeId(PYTHON_SOURCE_ROOT_TYPE)),
          strictDependencies = WorkspaceModelTargetLabelList(
            StrictDependencyCheckedType.OFF,
            emptyList(),
          ),
          entitySource = entitySource,
        )
      },
    )
  }

  private fun Sdk.toModuleDependencyItem(): ModuleDependencyItem = SdkDependency(SdkId(name, PYTHON_SDK_ID))

  private fun getContentRootEntities(
    context: WorkspaceImporterContext,
    target: RawBuildTarget,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntityBuilder> {
    val sourceContentRootEntities = getSourceContentRootEntities(context, target, entitySource, virtualFileUrlManager)
    val resourceContentRootEntities = getResourceContentRootEntities(target, entitySource, virtualFileUrlManager)

    return sourceContentRootEntities + resourceContentRootEntities
  }

  private fun getSourceContentRootEntities(
    context: WorkspaceImporterContext,
    target: RawBuildTarget,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntityBuilder> = computeSourceRootPaths(context, target)
      .map { it.toContentRoot(PYTHON_SOURCE_ROOT_TYPE, entitySource, virtualFileUrlManager) }

  private fun getResourceContentRootEntities(
    target: RawBuildTarget,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntityBuilder> = target.resources.getFiles()
    .map { resource -> resource.toContentRoot(PYTHON_RESOURCE_ROOT_TYPE, entitySource, virtualFileUrlManager) }
    .toList()

  private fun computeSourceRootPaths(context: WorkspaceImporterContext, target: RawBuildTarget): Set<Path> {
    val projectCtx = context.project.projectCtx
    // imports for generated files should be resolved against bazel-bin
    val basePaths = listOfNotNull(projectCtx.projectRootDir?.toNioPath(), projectCtx.bazelBinPath).distinct()
    val importRoots = target.assembleImportsPaths()
      .mapNotNull { importPath ->
        basePaths.firstNotNullOfOrNull { base ->
          base.resolve(importPath).normalize().takeIf { it.startsWith(base) && it.isDirectory() }
        }
      }
      .toSet()
    val individualFiles = target.allSources
      .filter { !it.isUnder(importRoots) }
      .toSet()
    return importRoots + individualFiles
  }

  private fun Path.toContentRoot(
    rootType: String,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): ContentRootEntityBuilder {
    val url = toVirtualFileUrl(virtualFileUrlManager)
    val sourceRootEntity = SourceRootEntity(
      url = url,
      rootTypeId = SourceRootTypeId(rootType),
      entitySource = entitySource,
    )
    return ContentRootEntity(
      url = url,
      excludedPatterns = emptyList(),
      entitySource = entitySource,
    ) {
      this.excludedUrls = emptyList()
      this.sourceRoots = listOf(sourceRootEntity)
    }
  }

  private suspend fun createSdkFromPython(
    interpreter: Path,
    project: Project,
    sdkName: String?,
  ): PyResult<Sdk> = createLocalSdkGuessingTypeByPath(interpreter, ProjectOnly(project), sdkName)

  private suspend fun getSystemSdk(taskId: TaskId, project: Project, taskConsole: TaskConsole): Sdk? {
    val systemPython = SystemPythonService().findSystemPythons().firstOrNull { it.pythonInfo.languageLevel.isPy3K }
                       ?: run {
                         taskConsole.addMessage(taskId, BazelPythonBackendBundle.message("python.not.found"))
                         return null
                       }
    logger.info("Detecting system SDK, found python $systemPython")
    return when (val result = createSdkFromPython(systemPython.pythonBinary, project, sdkName = null)) {
      is com.jetbrains.python.Result.Failure -> {
        taskConsole.addMessage(
          taskId,
          BazelPythonBackendBundle.message("python.cant.create.sdk", systemPython.pythonBinary, result.error.message),
        )
        null
      }

      is com.jetbrains.python.Result.Success -> result.result
    }
  }

  private fun WorkspaceTargetKey.toPythonModuleName(repoMapping: RepoMapping) = moduleNameByKey[this] ?: this.formatAsModuleName(repoMapping)
}

@ApiStatus.Internal
@VisibleForTesting
fun chooseSdkName(interpreter: PythonBinary, projectName: String): String =
  "$projectName-python-${StringUtils.md5Hash(interpreter.pathString, 5)}"
