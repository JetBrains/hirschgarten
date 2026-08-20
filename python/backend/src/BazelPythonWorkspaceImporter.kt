package com.intellij.bazel.python.backend

import com.intellij.bazel.python.backend.sync.PythonWorkspaceSyncConfig
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ContentRootEntityBuilder
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleDependency
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
import com.jetbrains.python.PyNames
import com.jetbrains.python.PythonBinary
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.sdk.ModuleOrProject.ProjectOnly
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.createLocalSdkGuessingTypeByPath
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.magicmetamodel.formatAsLibraryName
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetMerger
import org.jetbrains.bazel.sync.workspace.snapshot.allSources
import org.jetbrains.bazel.sync.workspace.snapshot.allTargets
import org.jetbrains.bazel.sync.workspace.snapshot.commonSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.filterBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.findBuildData
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleExtensionEntity
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabelList
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetSourceRootTypeId
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.utils.StringUtils
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

private const val PYTHON_SOURCE_ROOT_TYPE = "python-source"
private const val PYTHON_RESOURCE_ROOT_TYPE = "python-resource"
private val PYTHON_MODULE_TYPE = ModuleTypeId("PYTHON_MODULE")

internal class BazelPythonWorkspaceImporter : BazelWorkspaceImporter, BazelWorkspaceImporter.Named {
  companion object {
    internal val logger = logger<BazelPythonWorkspaceImporter>()
  }

  override val importerName: @NlsContexts.ProgressTitle String
    get() = BazelPythonBackendBundle.message("python.workspace.importer")

  private lateinit var commonSyncConfig: CommonWorkspaceSyncConfig
  private lateinit var pythonSyncConfig: PythonWorkspaceSyncConfig
  private lateinit var moduleNameByKey: Map<WorkspaceTargetKey, String>
  private lateinit var allPythonTargets: Map<WorkspaceTargetKey, BuildTarget>

  private var defaultInterpreter: Path? = null
  private var defaultVersion: String? = null
  private var externalSourceDependenciesByTarget: Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> = mapOf()

  override suspend fun import(
    context: WorkspaceImporterContext,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> = runCatching {
    when (phase) {
      WorkspaceImporterPhase.Initialize -> onInitialize(snapshot)
      is WorkspaceImporterPhase.WorkspaceApply -> onWorkspaceApply(context, snapshot, phase.builder, context.vfuManager, phase.entitySource)
      WorkspaceImporterPhase.PostProcessing -> onPostProcessing(context, snapshot)
      else -> WorkspaceImporterResult.Success
    }
  }

  private fun onInitialize(snapshot: WorkspaceSnapshot): WorkspaceImporterResult {
    commonSyncConfig = snapshot.syncConfigs.filterIsInstance<CommonWorkspaceSyncConfig>()
                         .firstOrNull() ?: throw IllegalStateException()
    pythonSyncConfig = snapshot.syncConfigs.filterIsInstance<PythonWorkspaceSyncConfig>()
                         .firstOrNull() ?: throw IllegalStateException()

    val importDepth = snapshot.commonSyncConfig.importDepth
    allPythonTargets = snapshot.targetGraph.findAllTargetsAtDepth(maxDepth = importDepth, useRelaxedDependencyExpansion = true)
      .mapNotNull { it.load(snapshot.targets, TargetLoadOptions.ALL) }
      .filter { it.hasBuildData<PythonBuildTarget>() }
      .let { targets ->
        WorkspaceTargetMerger(mergeFunctions = pythonTargetMergeFunctions)
          .mergeByTargetKey(targets = targets.toList())
      }
      .associateBy { it.key }

    moduleNameByKey = allPythonTargets.values
      .groupBy { it.key.label }
      .flatMap { (_, targets) ->
        when {
          targets.size == 1 -> {
            val key = targets.single().key
            listOf(key to key.formatAsModuleName(snapshot.repoMapping, withConfiguration = false))
          }

          else -> targets.map { it.key to it.key.formatAsModuleName(snapshot.repoMapping, withConfiguration = true) }
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

    externalSourceDependenciesByTarget =
      snapshot
        .allTargets
        .filterBuildTarget<PythonBuildTarget>()
        .map { it.first.key }
        .associateWith { workspaceTargetKey ->
          snapshot.targetGraph.findAllTransitiveSuccessorsWithoutRootTargets(workspaceTargetKey)
            .mapNotNull { it.load(snapshot.targets, TargetLoadOptions.ALL) }
            .filterBuildTarget<PythonBuildTarget>()
            .filter { it.second.externalSources?.isEmpty() == false }
            .map { it.first.key }
            .distinct()
            .toList()
        }

    return WorkspaceImporterResult.Success
  }

  private fun onWorkspaceApply(
    context: WorkspaceImporterContext,
    snapshot: WorkspaceSnapshot,
    builder: MutableEntityStorage,
    vfuManager: VirtualFileUrlManager,
    entitySource: EntitySource,
  ): WorkspaceImporterResult {
    val allTargetsByKey = snapshot.allTargets.associateBy { it.key }
    val sourceDependencyLibraries =
      allPythonTargets.keys
        .flatMap { externalSourceDependenciesByTarget[it].orEmpty() }
        .distinct()
        .mapNotNull { dependencyKey ->
          // calculate one external source path list per iteration to avoid keeping everything in memory at once
          val dependencyTarget = allTargetsByKey[dependencyKey] ?: return@mapNotNull null
          val externalSources = getExternalSourcePaths(dependencyTarget)
          if (externalSources.isEmpty()) return@mapNotNull null
          addSourceDependencyLibrary(builder, dependencyKey, snapshot.repoMapping, externalSources, entitySource, vfuManager)
            ?.let { dependencyKey to it }
        }.toMap()

    for ((targetKey, target) in allPythonTargets) {
      addModuleEntityFromTarget(
        context = context,
        builder = builder,
        repoMapping = snapshot.repoMapping,
        target = target,
        moduleName = targetKey.toPythonModuleName(snapshot.repoMapping),
        entitySource = entitySource,
        virtualFileUrlManager = vfuManager,
        sourceDependencyLibraries = externalSourceDependenciesByTarget[targetKey].orEmpty().mapNotNull { sourceDependencyLibraries[it] },
      )
    }
    return WorkspaceImporterResult.Success
  }

  private fun getExternalSourcePaths(target: BuildTarget): List<Path> {
    val pythonTarget = target.findBuildData<PythonBuildTarget>() ?: return emptyList()
    return pythonTarget.externalSources?.getFiles()?.distinct()?.toList().orEmpty()
  }

  private suspend fun onPostProcessing(context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): WorkspaceImporterResult {
    /**
     * Because of PY-86494, PythonSdkUpdater fails to add SDK paths unless there's at least one module in the project.
     * Hence, we're forced to do it in post-processing, after WSM has been applied already.
     */
    calculateAndAddSdksWithProgress(context, snapshot)

    val pyTargets = allPythonTargets.values
      .filter { it.hasBuildData<PythonBuildTarget>() }
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
      .associateBy { (target, _) -> target.key }
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

  private fun addSourceDependencyLibrary(
    builder: MutableEntityStorage,
    dependencyTarget: WorkspaceTargetKey,
    repoMapping: RepoMapping,
    sourceDependencies: List<Path>,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): LibraryEntity? {
    val roots =
      sourceDependencies.distinct().map {
        LibraryRoot(
          url = it.toVirtualFileUrl(virtualFileUrlManager),
          type = LibraryRootTypeId.SOURCES,
        )
      }
    if (roots.isEmpty()) {
      return null
    }

    val name = dependencyTarget.formatAsLibraryName(repoMapping, withFullKey = true)
    val tableId = LibraryTableId.ProjectLibraryTableId
    return builder.resolve(LibraryId(name, tableId))
           ?: builder.addEntity(
             LibraryEntity(
               name = name,
               tableId = tableId,
               roots = roots,
               entitySource = entitySource,
             ),
           )
  }

  private fun addModuleEntityFromTarget(
    context: WorkspaceImporterContext,
    builder: MutableEntityStorage,
    repoMapping: RepoMapping,
    target: BuildTarget,
    moduleName: String,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
    sourceDependencyLibraries: List<LibraryEntity> = emptyList(),
  ): ModuleEntity {
    val contentRoots = getContentRootEntities(context, target, entitySource, virtualFileUrlManager)

    val libraryDependencies =
      sourceDependencyLibraries.map {
        LibraryDependency(it.symbolicId, true, DependencyScope.COMPILE)
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

    val pythonTarget = target.findBuildData<PythonBuildTarget>()
    val interpreter = pythonTarget?.interpreter
                      ?: defaultInterpreter
    val sdkName = interpreter?.let { chooseSdkName(interpreter, context.project.name) }
                  ?: chooseSystemSdkName(context.project.name)
    val sdkDependency = SdkDependency(SdkId(sdkName, PyNames.PYTHON_SDK_ID_NAME))

    val allDependencies =
      buildList {
        addAll(dependencies)
        addAll(listOf(ModuleSourceDependency, sdkDependency))
        addAll(libraryDependencies)
      }

    return builder.addEntity(
      ModuleEntity(
        name = moduleName,
        dependencies = allDependencies,
        entitySource = entitySource,
      ) {
        this.type = PYTHON_MODULE_TYPE
        this.contentRoots = contentRoots
        this.bazelModuleExtension = BazelModuleExtensionEntity(
          _targetKey = WorkspaceModelTargetKey.of(target.key),
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

  private fun getContentRootEntities(
    context: WorkspaceImporterContext,
    target: BuildTarget,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntityBuilder> {
    val sourceContentRootEntities = getSourceContentRootEntities(context, target, entitySource, virtualFileUrlManager)
    val resourceContentRootEntities = getResourceContentRootEntities(target, entitySource, virtualFileUrlManager)

    return sourceContentRootEntities + resourceContentRootEntities
  }

  private fun getSourceContentRootEntities(
    context: WorkspaceImporterContext,
    target: BuildTarget,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntityBuilder> = computeSourceRootPaths(context, target)
    .map { it.toContentRoot(PYTHON_SOURCE_ROOT_TYPE, entitySource, virtualFileUrlManager) }

  private fun getResourceContentRootEntities(
    target: BuildTarget,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<ContentRootEntityBuilder> = target.resources.getFiles()
    .map { resource -> resource.toContentRoot(PYTHON_RESOURCE_ROOT_TYPE, entitySource, virtualFileUrlManager) }
    .toList()

  private fun computeSourceRootPaths(context: WorkspaceImporterContext, target: BuildTarget): Set<Path> {
    val projectCtx = context.project.projectCtx
    // imports for generated files should be resolved against bazel-bin
    val basePaths = listOfNotNull(projectCtx.projectRootDir?.toNioPath(), projectCtx.bazelBinPath).distinct()
    val importRoots =
      PythonImportUtils.assembleExplicitImportsPaths(target)
        .mapNotNull { importPath ->
          basePaths.firstNotNullOfOrNull { base ->
            base.resolve(importPath).normalize().takeIf { it.startsWith(base) && it.isDirectory() }
          }
        }.toSet()
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

  private fun WorkspaceTargetKey.toPythonModuleName(repoMapping: RepoMapping) =
    moduleNameByKey[this] ?: this.formatAsModuleName(repoMapping)
}

@ApiStatus.Internal
@VisibleForTesting
fun chooseSdkName(interpreter: PythonBinary, projectName: String): String =
  "$projectName-python-${StringUtils.md5Hash(interpreter.pathString, 5)}"

private fun chooseSystemSdkName(projectName: String): String =
  "$projectName-python"
