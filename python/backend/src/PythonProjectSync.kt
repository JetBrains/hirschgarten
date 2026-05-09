package com.intellij.bazel.python.backend

import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
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
import com.jetbrains.python.Result
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.sdk.ModuleOrProject.ProjectOnly
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.createLocalSdkGuessingTypeByPath
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.utils.StringUtils
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import java.nio.file.Path
import kotlin.collections.get
import kotlin.io.path.pathString

private const val PYTHON_SDK_ID = "PythonSDK"
private const val PYTHON_SOURCE_ROOT_TYPE = "python-source"
private const val PYTHON_RESOURCE_ROOT_TYPE = "python-resource"
private val PYTHON_MODULE_TYPE = ModuleTypeId("PYTHON_MODULE")

private val logger = fileLogger()

@ApiStatus.Internal
class PythonProjectSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.isPythonSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Process Python targets") {
      val targets = environment.workspace.targets
      val pythonTargets = targets.filter {
        it.kind.languageClasses.contains(LanguageClass.PYTHON)
      }
      val virtualFileUrlManager = environment.project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

      val sdks = calculateAndAddSdksWithProgress(pythonTargets, environment)

      var defaultSystemSdk: Sdk? = null
      pythonTargets.forEach {
        val moduleName = it.id.formatAsModuleName(environment.project)
        val moduleSourceEntity = BazelModuleEntitySource(moduleName)
        val target = extractPythonBuildTarget(it) ?: return@forEach
        val sourceDependencyLibrary =
          calculateSourceDependencyLibrary(it.id, target.sourceDependencies, moduleSourceEntity, virtualFileUrlManager)

        val sdk = sdks[it.id] ?: defaultSystemSdk ?: getSystemSdk(environment.taskId, environment.project)?.also { systemSdk ->
          defaultSystemSdk = systemSdk
        }
        addModuleEntityFromTarget(
          builder = environment.diff,
          target = it,
          moduleName = moduleName,
          entitySource = moduleSourceEntity,
          virtualFileUrlManager = virtualFileUrlManager,
          project = environment.project,
          sdk = sdk,
          sourceDependencyLibrary = sourceDependencyLibrary,
        )
      }
      environment.project.service<PythonResolveIndexService>().updatePythonResolveIndex(pythonTargets, environment.server.outFileHardLinks)
    }
  }

  private suspend fun calculateAndAddSdksWithProgress(
    targets: List<BuildTarget>,
    environment: ProjectSyncHookEnvironment,
  ): Map<Label, Sdk?> =
    environment.progressReporter.indeterminateStep(text = BazelPythonBackendBundle.message("progress.bar.calculate.python.sdk.infos")) {
      environment.project.syncConsole.withSubtask(
        subtaskId = environment.taskId.subTask("calculate-and-add-all-python-sdk-infos"),
        message = BazelPythonBackendBundle.message("console.task.model.calculate.python.sdks"),
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
    // Before this change we had toString() here so in case of null there would be "null"
    // This !! fails fast
    // TODO: Make make interpreter non-null
    val interpreter = pythonTarget.interpreter!!
    val sdkName = chooseSdkName(interpreter, project.name)
    val sdkTable = ProjectJdkTable.getInstance()

    val existingSdk = sdkTable.findJdk(sdkName, PythonSdkType.getInstance().toString())
    if (existingSdk != null) return existingSdk

    return when (val r = createSdkFromPython(interpreter, project, sdkName)) {
      is Result.Failure -> {
        // TODO: Process error somehow
        error("Failed to create SDK for $interpreter: ${r.error}")
      }

      is Result.Success -> r.result
    }
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
}

private suspend fun createSdkFromPython(
  interpreter: Path,
  project: Project,
  sdkName: String?,
): PyResult<Sdk> = createLocalSdkGuessingTypeByPath(interpreter, ProjectOnly(project), sdkName)

private suspend fun getSystemSdk(taskId: TaskId, project: Project): Sdk? {
  val systemPython = SystemPythonService().findSystemPythons().firstOrNull { it.pythonInfo.languageLevel.isPy3K }
                     ?: run {
                       project.syncConsole.addMessage(taskId, BazelPluginBundle.message("python.not.found"))
                       return null
                     }
  logger.info("Detecting system SDK, found python $systemPython")
  return when (val r = createSdkFromPython(systemPython.pythonBinary, project, sdkName = null)) {
    is Result.Failure -> {
      project.syncConsole.addMessage(taskId, BazelPluginBundle.message("python.cant.create.sdk", systemPython.pythonBinary, r.error.message))
      null
    }

    is Result.Success -> r.result
  }
}

@ApiStatus.Internal
@VisibleForTesting
fun chooseSdkName(interpreter: PythonBinary, projectName: String): String =
  "$projectName-python-${StringUtils.md5Hash(interpreter.pathString, 5)}"

