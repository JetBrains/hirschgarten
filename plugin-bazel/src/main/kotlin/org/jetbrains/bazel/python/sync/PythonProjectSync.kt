package org.jetbrains.bazel.python.sync

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.platform.backend.workspace.WorkspaceModel
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
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.guessedLanguageLevel
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.python.resolve.PythonResolveIndexService
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bsp.protocol.utils.StringUtils
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import java.nio.file.Path

private const val PYTHON_SDK_ID = "PythonSDK"
private const val PYTHON_SOURCE_ROOT_TYPE = "python-source"
private const val PYTHON_RESOURCE_ROOT_TYPE = "python-resource"
private val PYTHON_MODULE_TYPE = ModuleTypeId("PYTHON_MODULE")

private val LOG = logger<PythonProjectSync>()

@ApiStatus.Internal

class PythonProjectSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.isPythonSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Process Python targets") {
      val targets = environment.workspace.targets
      val pythonTargets = targets.calculatePythonTargets()
      val virtualFileUrlManager = environment.project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

      val sdks = calculateAndAddSdksWithProgress(pythonTargets, environment)
      val defaultSdk = null

      pythonTargets.forEach {
        val moduleName = it.id.formatAsModuleName(environment.project)
        val moduleSourceEntity = BazelModuleEntitySource(moduleName)
        val target = it.data as? PythonBuildTarget ?: return@forEach
        val sourceDependencyLibrary =
          calculateSourceDependencyLibrary(it.id, target.sourceDependencies, moduleSourceEntity, virtualFileUrlManager)

        addModuleEntityFromTarget(
          builder = environment.diff,
          target = it as RawBuildTarget,
          moduleName = moduleName,
          entitySource = moduleSourceEntity,
          virtualFileUrlManager = virtualFileUrlManager,
          project = environment.project,
          sdk = sdks[it.id] ?: defaultSdk,
          sourceDependencyLibrary = sourceDependencyLibrary,
        )
      }

      // Create a fallback module for non-target Python files in included directories
      createFallbackPythonModuleIfNeeded(
        environment = environment,
        pythonTargets = pythonTargets,
        sdks = sdks,
        defaultSdk = defaultSdk,
        virtualFileUrlManager = virtualFileUrlManager,
      )

      environment.project.service<PythonResolveIndexService>().updatePythonResolveIndex(targets.toList())
    }
  }

  private fun List<RawBuildTarget>.calculatePythonTargets(): List<BuildTarget> =
    this.filter {
      it.kind.languageClasses.contains(LanguageClass.PYTHON)
    }

  private suspend fun calculateAndAddSdksWithProgress(
    targets: List<BuildTarget>,
    environment: ProjectSyncHookEnvironment,
  ): Map<Label, Sdk?> =
    environment.progressReporter.indeterminateStep(text = BazelPluginBundle.message("progress.bar.calculate.python.sdk.infos")) {
      environment.project.syncConsole.withSubtask(
        subtaskId = environment.taskId.subTask("calculate-and-add-all-python-sdk-infos"),
        message = BazelPluginBundle.message("console.task.model.calculate.python.sdks"),
      ) {
        calculateAndAddSdks(targets, environment.project)
      }
    }

  private suspend fun calculateAndAddSdks(targets: List<BuildTarget>, project: Project): Map<Label, Sdk?> {
    val pythonTargetsByLabel =
      targets
        .associateWith { extractPythonBuildTarget(it) }
        .mapKeys { it.key.id }
    val sdksByPythonTarget =
      pythonTargetsByLabel.values
        .filterNotNull()
        .filter { it.interpreter != null }
        .distinct()
        .associateWith { findOrAddSdk(it, project) }

    return pythonTargetsByLabel.mapValues { sdksByPythonTarget[it.value] }
  }

  private suspend fun findOrAddSdk(pythonTarget: PythonBuildTarget, project: Project): Sdk {
    val sdkName = chooseSdkName(pythonTarget, project.name)
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

  private fun chooseSdkName(interpreter: PythonBuildTarget, projectName: String): String =
    "$projectName-python-${StringUtils.md5Hash(interpreter.interpreter.toString(), 5)}"

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
          module = ModuleId(it.label.formatAsModuleName(project)),
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
  ): List<ContentRootEntityBuilder> =
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

  /**
   * Creates fallback Python modules for files in included directories that are not part of any Python target.
   * This ensures that Python files outside of targets still get a Python SDK instead of falling back to the
   * project-level Java SDK.
   * Creates one module per root-level folder to allow different folder-level settings.
   */
  private suspend fun createFallbackPythonModuleIfNeeded(
    environment: ProjectSyncHookEnvironment,
    pythonTargets: List<BuildTarget>,
    sdks: Map<Label, Sdk?>,
    defaultSdk: Sdk?,
    virtualFileUrlManager: VirtualFileUrlManager,
  ) {
    // Prefer any Python SDK from Bazel sync, fall back to system SDK
    // Validate that we only use actual Python SDKs, not JDK or other SDK types
    val pythonSdkType = PythonSdkType.getInstance()
    val pythonSdk = sdks.values.firstOrNull { sdk ->
      sdk != null && sdk.sdkType == pythonSdkType
    } ?: defaultSdk?.takeIf { it.sdkType == pythonSdkType }
    if (pythonSdk == null) {
      return
    }
    val builder = environment.diff.workspaceModelDiff.mutableEntityStorage

    // Get the BazelProjectDirectoriesEntity to know which directories are included
    // Query from the mutableEntityStorage being built, not the current snapshot
    val directoriesEntity = builder.entities(BazelProjectDirectoriesEntity::class.java).firstOrNull()
    if (directoriesEntity == null || directoriesEntity.includedRoots.isEmpty()) {
      // No included directories, nothing to do
      return
    }

    // Collect all directories that are already covered by Python targets
    val targetCoveredUrls = pythonTargets.flatMap { target ->
      (target as RawBuildTarget).sources.map { it.path.toVirtualFileUrl(virtualFileUrlManager) }
    }.toSet()

    // Find included roots that are not covered by any Python target
    val uncoveredRoots = directoriesEntity.includedRoots.filterNot { includedRoot ->
      // Check if this included root is already covered by a target
      targetCoveredUrls.any { targetUrl ->
        targetUrl.url.startsWith(includedRoot.url) || includedRoot.url.startsWith(targetUrl.url)
      }
    }

    if (uncoveredRoots.isEmpty()) {
      // All included directories are covered by targets
      return
    }

    // Create one module per root-level folder to allow different folder-level settings
    uncoveredRoots.forEach { rootUrl ->
      // Extract folder name from URL (e.g., "file:///path/to/root/folder1" -> "folder1")
      val folderName = rootUrl.url.trimEnd('/').substringAfterLast('/')
      val fallbackModuleName = "${environment.project.name}.python.$folderName"
      val fallbackEntitySource = BazelModuleEntitySource(fallbackModuleName)

      // Create content root for this directory
      val sourceRootEntity = SourceRootEntity(
        url = rootUrl,
        rootTypeId = SourceRootTypeId(PYTHON_SOURCE_ROOT_TYPE),
        entitySource = fallbackEntitySource,
      )
      val contentRoot = ContentRootEntity(
        url = rootUrl,
        excludedPatterns = emptyList(),
        entitySource = fallbackEntitySource,
      ) {
        this.excludedUrls = emptyList()
        this.sourceRoots = listOf(sourceRootEntity)
      }

      // Create the fallback module with Python SDK from Bazel sync
      // Use JAVA module type (default) with Python SDK - IntelliJ respects Python folders this way
      // ModuleSourceDependency is crucial - it's the "<module source>" entry that tells IntelliJ
      // to recognize the module's own content roots as source folders
      val dependencies = listOf(
        ModuleSourceDependency,
        pythonSdk.toModuleDependencyItem(),
      )

      builder.addEntity(
        ModuleEntity(
          name = fallbackModuleName,
          dependencies = dependencies,
          entitySource = fallbackEntitySource,
        ) {
          // Use default JAVA module type - IntelliJ will respect Python folders with Python SDK
          this.contentRoots = listOf(contentRoot)
        },
      )

      LOG.info("Created fallback Python module: $fallbackModuleName for folder: $folderName with Python SDK: ${pythonSdk.name}")
    }
  }
}

/**
 * Finds a suitable Python SDK using a comprehensive fallback strategy:
 * 1. First, check all Python SDKs already configured in IntelliJ
 * 2. If none found, detect system-wide Python installations
 * 3. Prefer Python 3.x over Python 2.x
 */
private fun findPythonSdk(project: Project): Sdk? {
  val pythonSdkType = PythonSdkType.getInstance()

  // Strategy 1: Check already configured Python SDKs in IntelliJ
  LOG.info("Searching for Python SDK: checking configured SDKs")
  val configuredSdks = PythonSdkUtil.getAllSdks().filter { it.sdkType == pythonSdkType }
  if (configuredSdks.isNotEmpty()) {
    val preferredSdk = configuredSdks.firstOrNull {
      (it as? PyDetectedSdk)?.guessedLanguageLevel?.isPy3K == true
    } ?: configuredSdks.first()
    LOG.info("Found configured Python SDK: ${preferredSdk.name}")
    return preferredSdk
  }

  LOG.warn("No Python SDK found on system")
  return null
}
