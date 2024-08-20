package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.JpsFileDependentEntitySource
import com.intellij.platform.workspace.jps.JpsFileEntitySource
import com.intellij.platform.workspace.jps.JpsGlobalFileEntitySource
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.jetbrains.bsp.protocol.BazelBuildServer
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.DirectoryItem
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.bsp.protocol.utils.extractAndroidBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics
import org.jetbrains.plugins.bsp.android.AndroidSdk
import org.jetbrains.plugins.bsp.android.AndroidSdkGetterExtension
import org.jetbrains.plugins.bsp.android.androidSdkGetterExtension
import org.jetbrains.plugins.bsp.android.androidSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.PythonSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.flow.open.projectSyncHook
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.androidJarToAndroidSdkName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.scalaVersionToScalaSdkName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesJava
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesScala
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBuildTargetInfo
import org.jetbrains.plugins.bsp.performance.testing.bspTracer
import org.jetbrains.plugins.bsp.scala.sdk.ScalaSdk
import org.jetbrains.plugins.bsp.scala.sdk.scalaSdkExtension
import org.jetbrains.plugins.bsp.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.utils.SdkUtils
import org.jetbrains.plugins.bsp.utils.findLibraryNameProvider
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.utils.orDefault
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspEntitySource
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.Path

private const val COLLECT_PROJECT_TASK_ID = "bsp-collect-project-task"

public data class PythonSdk(
  val name: String,
  val interpreterUri: String,
  val dependencies: List<DependencySourcesItem>,
)

public class CollectProjectDetailsTask(
  project: Project,
  private val taskId: Any,
  private val name: String,
  private val cancelable: Boolean,
  private val buildProject: Boolean,
) : BspServerTask<ProjectDetails>("collect project details", project) {
  private val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole

  private var builder: MutableEntityStorage? = null

  private var uniqueJavaHomes: Set<String>? = null

  private lateinit var javacOptions: Map<String, String>

  private var pythonSdks: Set<PythonSdk>? = null

  private var scalaSdks: Set<ScalaSdk>? = null

  private var androidSdks: Set<AndroidSdk>? = null

  public suspend fun execute() {
    saveAllFiles()
    doExecute()
  }

  private suspend fun doExecute() {
    withBackgroundProgress(project, name, cancelable) {
      reportSequentialProgress { reporter ->
        val projectDetails =
          reporter.sizedStep(workSize = 50, text = BspPluginBundle.message("progress.bar.collect.project.details")) {
            calculateProjectDetailsSubtask(buildProject)
          } ?: return@reportSequentialProgress

        reporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.jdk.infos")) {
          calculateAllUniqueJdkInfosSubtask(projectDetails)
          uniqueJavaHomes.orEmpty().also {
            if (it.isNotEmpty()) {
              projectDetails.defaultJdkName = project.name.projectNameToJdkName(it.first())
            } else {
              projectDetails.defaultJdkName = SdkUtils.getProjectJdkOrMostRecentJdk(project)?.name
            }
          }
        }

        if (BspFeatureFlags.isPythonSupportEnabled && pythonSdkGetterExtensionExists()) {
          reporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.python.sdk.infos")) {
            calculateAllPythonSdkInfosSubtask(projectDetails)
          }
        }

        if (BspFeatureFlags.isScalaSupportEnabled && scalaSdkExtensionExists()) {
          reporter.indeterminateStep(text = "Calculating all unique scala sdk infos") {
            calculateAllScalaSdkInfosSubtask(projectDetails)
          }
        }

        if (BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists()) {
          reporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.android.sdk.infos")) {
            calculateAllAndroidSdkInfosSubtask(projectDetails)
          }
        }

        reporter.sizedStep(workSize = 25, text = BspPluginBundle.message("progress.bar.update.internal.model")) {
          updateInternalModelSubtask(projectDetails)
        }

        reporter.sizedStep(workSize = 25, text = BspPluginBundle.message("progress.bar.post.processing")) {
          postprocessingSubtask()
        }
      }
    }
  }

  private suspend fun calculateProjectDetailsSubtask(buildProject: Boolean) =
    bspTracer.spanBuilder("collect.project.details.ms").use {
      connectAndExecuteWithServer { server, capabilities ->
        collectModel(server, capabilities, buildProject)
      }
    }

  private suspend fun collectModel(
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    buildProject: Boolean,
  ): ProjectDetails =
    try {
      bspSyncConsole.startSubtask(
        this.taskId,
        COLLECT_PROJECT_TASK_ID,
        BspPluginBundle.message("console.task.model.collect.in.progress"),
      )

      val projectDetails =
        calculateProjectDetailsWithCapabilities(
          project = project,
          server = server,
          buildServerCapabilities = capabilities,
          projectRootDir = project.rootDir.url,
          buildProject = buildProject,
        )

      bspSyncConsole.finishSubtask(COLLECT_PROJECT_TASK_ID, BspPluginBundle.message("console.task.model.collect.success"))

      projectDetails
    } catch (e: Exception) {
      if (e is CancellationException) {
        bspSyncConsole.finishSubtask(
          COLLECT_PROJECT_TASK_ID,
          BspPluginBundle.message("console.task.model.collect.cancelled"),
          FailureResultImpl(),
        )
      } else {
        bspSyncConsole.finishSubtask(
          COLLECT_PROJECT_TASK_ID,
          BspPluginBundle.message("console.task.model.collect.failed"),
          FailureResultImpl(e),
        )
      }
      throw e
    }

  private suspend fun withSubtask(
    subtaskId: String,
    message: String,
    block: suspend (subtaskId: String) -> Unit,
  ) {
    bspSyncConsole.startSubtask(taskId, subtaskId, message)
    block(subtaskId)
    bspSyncConsole.finishSubtask(subtaskId, message)
  }

  private suspend fun calculateAllUniqueJdkInfosSubtask(projectDetails: ProjectDetails) =
    withSubtask(
      "calculate-all-unique-jdk-infos",
      BspPluginBundle.message("console.task.model.calculate.jdks.infos"),
    ) {
      uniqueJavaHomes =
        bspTracer.spanBuilder("calculate.all.unique.jdk.infos.ms").use {
          calculateAllUniqueJavaHomes(projectDetails)
        }
    }

  private fun calculateAllUniqueJavaHomes(projectDetails: ProjectDetails): Set<String> =
    projectDetails.targets
      .mapNotNull(::extractJvmBuildTarget)
      .map { it.javaHome }
      .toSet()

  private suspend fun calculateAllScalaSdkInfosSubtask(projectDetails: ProjectDetails) =
    withSubtask(
      "calculate-all-scala-sdk-infos",
      BspPluginBundle.message("console.task.model.calculate.scala.sdk.infos"),
    ) {
      scalaSdks =
        bspTracer.spanBuilder("calculate.all.scala.sdk.infos.ms").use {
          calculateAllScalaSdkInfos(projectDetails)
        }
    }

  private fun calculateAllScalaSdkInfos(projectDetails: ProjectDetails): Set<ScalaSdk> =
    projectDetails.targets
      .mapNotNull {
        createScalaSdk(it)
      }.toSet()

  private fun createScalaSdk(target: BuildTarget): ScalaSdk? =
    extractScalaBuildTarget(target)
      ?.let {
        ScalaSdk(
          name = it.scalaVersion.scalaVersionToScalaSdkName(),
          scalaVersion = it.scalaVersion,
          sdkJars = it.jars,
        )
      }

  private suspend fun calculateAllPythonSdkInfosSubtask(projectDetails: ProjectDetails) =
    withSubtask(
      "calculate-all-python-sdk-infos",
      BspPluginBundle.message("console.task.model.calculate.python.sdks.done"),
    ) {
      runInterruptible {
        pythonSdks =
          bspTracer.spanBuilder("calculate.all.python.sdk.infos.ms").use {
            calculateAllPythonSdkInfos(projectDetails)
          }
      }
    }

  private fun createPythonSdk(target: BuildTarget, dependenciesSources: List<DependencySourcesItem>): PythonSdk? =
    extractPythonBuildTarget(target)?.let {
      if (it.interpreter != null && it.version != null) {
        PythonSdk(
          name = "${target.id.uri}-${it.version}",
          interpreterUri = it.interpreter,
          dependencies = dependenciesSources,
        )
      } else {
        pythonSdkGetterExtension()
          ?.getSystemSdk()
          ?.let { sdk ->
            PythonSdk(
              name = "${target.id.uri}-detected-PY3",
              interpreterUri = Path(sdk.homePath!!).toUri().toString(),
              dependencies = dependenciesSources,
            )
          }
      }
    }

  private fun calculateAllPythonSdkInfos(projectDetails: ProjectDetails): Set<PythonSdk> =
    projectDetails.targets
      .mapNotNull {
        createPythonSdk(it, projectDetails.dependenciesSources.filter { a -> a.target.uri == it.id.uri })
      }.toSet()

  private suspend fun calculateAllAndroidSdkInfosSubtask(projectDetails: ProjectDetails) =
    withSubtask(
      "calculate-all-android-sdk-infos",
      BspPluginBundle.message("progress.bar.calculate.android.sdk.infos"),
    ) {
      androidSdks =
        bspTracer.spanBuilder("calculate.all.android.sdk.infos.ms").use {
          calculateAllAndroidSdkInfos(projectDetails)
        }
    }

  private fun calculateAllAndroidSdkInfos(projectDetails: ProjectDetails): Set<AndroidSdk> =
    projectDetails.targets
      .mapNotNull { createAndroidSdk(it) }
      .toSet()

  private fun createAndroidSdk(target: BuildTarget): AndroidSdk? =
    extractAndroidBuildTarget(target)?.androidJar?.let { androidJar ->
      AndroidSdk(
        name = androidJar.androidJarToAndroidSdkName(),
        androidJar = androidJar,
      )
    }

  private suspend fun updateInternalModelSubtask(projectDetails: ProjectDetails) {
    withSubtask("calculate-project-structure", BspPluginBundle.message("console.task.model.calculate.structure")) {
      runInterruptible {
        val projectBasePath = project.rootDir.toNioPath()
        val moduleNameProvider = project.findModuleNameProvider().orDefault()
        val libraryNameProvider = project.findLibraryNameProvider().orDefault()
        val libraryGraph = LibraryGraph(projectDetails.libraries.orEmpty())

        val libraries =
          bspTracer.spanBuilder("create.libraries.ms").use {
            libraryGraph.createLibraries(libraryNameProvider)
          }

        val libraryModules =
          bspTracer.spanBuilder("create.library.modules.ms").use {
            libraryGraph.createLibraryModules(libraryNameProvider, projectDetails.defaultJdkName)
          }

        val targetIdToModuleDetails =
          bspTracer.spanBuilder("create.module.details.ms").use {
            val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, libraryGraph)
            projectDetails.targetIds.associateWith { transformer.moduleDetailsForTargetId(it) }
          }

        val targetIdToModuleEntitiesMap =
          bspTracer.spanBuilder("create.target.id.to.module.entities.map.ms").use {
            val targetIdToTargetInfo = projectDetails.targets.associate { it.id to it.toBuildTargetInfo() }
            val targetIdToModuleEntityMap =
              TargetIdToModuleEntitiesMap(
                projectDetails = projectDetails,
                targetIdToModuleDetails = targetIdToModuleDetails,
                targetIdToTargetInfo = targetIdToTargetInfo,
                projectBasePath = projectBasePath,
                moduleNameProvider = moduleNameProvider,
                libraryNameProvider = libraryNameProvider,
                hasDefaultPythonInterpreter = BspFeatureFlags.isPythonSupportEnabled,
                isAndroidSupportEnabled = BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
              )

            project.temporaryTargetUtils.saveTargets(
              targetIdToTargetInfo,
              targetIdToModuleEntityMap,
              targetIdToModuleDetails,
              libraries,
              libraryModules,
            )

            targetIdToModuleEntityMap
          }

        bspTracer.spanBuilder("load.modules.ms").use {
          val workspaceModel = WorkspaceModel.getInstance(project)
          val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()

          this.builder = MutableEntityStorage.create()
          val workspaceModelUpdater =
            WorkspaceModelUpdater.create(
              builder!!,
              virtualFileUrlManager,
              projectBasePath,
              project,
              BspFeatureFlags.isPythonSupportEnabled,
              BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
            )

          val modulesToLoad = targetIdToModuleEntitiesMap.values.toList()

          workspaceModelUpdater.loadModules(modulesToLoad + project.temporaryTargetUtils.getAllLibraryModules())
          workspaceModelUpdater.loadLibraries(project.temporaryTargetUtils.getAllLibraries())
          workspaceModelUpdater
            .loadDirectories(projectDetails.directories, projectDetails.outputPathUris, virtualFileUrlManager)
          calculateAllJavacOptions(modulesToLoad)
        }
      }
    }
  }

  private fun WorkspaceModelUpdater.loadDirectories(
    directories: WorkspaceDirectoriesResult,
    outputPathUris: List<String>,
    virtualFileUrlManager: VirtualFileUrlManager,
  ) {
    val includedDirectories = directories.includedDirectories.map { it.toVirtualFileUrl(virtualFileUrlManager) }
    val excludedDirectories = directories.excludedDirectories.map { it.toVirtualFileUrl(virtualFileUrlManager) }
    val outputPaths = outputPathUris.map { virtualFileUrlManager.getOrCreateFromUrl(it) }

    loadDirectories(includedDirectories, excludedDirectories + outputPaths)
  }

  private fun DirectoryItem.toVirtualFileUrl(virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl =
    virtualFileUrlManager.getOrCreateFromUrl(uri)

  private fun calculateAllJavacOptions(modulesToLoad: List<Module>) {
    javacOptions =
      modulesToLoad
        .asSequence()
        .filterIsInstance<JavaModule>()
        .mapNotNull { module ->
          module.javaAddendum?.javacOptions?.takeIf { it.isNotEmpty() }?.let { javacOptions ->
            module.getModuleName() to javacOptions.joinToString(" ")
          }
        }.toMap()
  }

  private suspend fun postprocessingSubtask() {
    // This order is strict as now SDKs also use the workspace model,
    // updating jdks before applying the project model will render the action to fail.
    // This will be handled properly after this ticket:
    // https://youtrack.jetbrains.com/issue/BAZEL-426/Configure-JDK-using-workspace-model-API-instead-of-ProjectJdkTable
    applyChangesOnWorkspaceModel()
    project.temporaryTargetUtils.fireListeners()
    addBspFetchedJdks()
    addBspFetchedJavacOptions()

    if (BspFeatureFlags.isPythonSupportEnabled) {
      addBspFetchedPythonSdks()
    }

    if (BspFeatureFlags.isScalaSupportEnabled) {
      addBspFetchedScalaSdks()
    }

    if (BspFeatureFlags.isAndroidSupportEnabled) {
      addBspFetchedAndroidSdks()
    }

    VirtualFileManager.getInstance().asyncRefresh()
    project.refreshKotlinHighlighting()
  }

  private suspend fun addBspFetchedJdks() =
    withSubtask(
      "add-bsp-fetched-jdks",
      BspPluginBundle.message("console.task.model.add.fetched.jdks"),
    ) {
      bspTracer.spanBuilder("add.bsp.fetched.jdks.ms").useWithScope {
        uniqueJavaHomes?.forEach {
          SdkUtils.addJdkIfNeeded(
            projectName = project.name,
            javaHomeUri = it,
          )
        }
      }
    }

  private suspend fun addBspFetchedScalaSdks() {
    scalaSdkExtension()?.let { extension ->
      withSubtask("add-bsp-fetched-scala-sdks", BspPluginBundle.message("console.task.model.add.scala.fetched.sdks")) {
        val modifiableProvider = IdeModifiableModelsProviderImpl(project)

        writeAction {
          scalaSdks?.forEach { extension.addScalaSdk(it, modifiableProvider) }
          modifiableProvider.commit()
        }
      }
    }
  }

  private suspend fun addBspFetchedPythonSdks() {
    pythonSdkGetterExtension()?.let { extension ->
      withSubtask(
        "add-bsp-fetched-python-sdks",
        BspPluginBundle.message("console.task.model.add.python.fetched.sdks"),
      ) {
        bspTracer.spanBuilder("add.bsp.fetched.python.sdks.ms").useWithScope {
          pythonSdks?.forEach { addPythonSdkIfNeeded(it, extension) }
        }
      }
    }
  }

  private suspend fun addPythonSdkIfNeeded(pythonSdk: PythonSdk, pythonSdkGetterExtension: PythonSdkGetterExtension) {
    val sdk =
      runInterruptible {
        pythonSdkGetterExtension.getPythonSdk(
          pythonSdk,
          WorkspaceModel.getInstance(project).getVirtualFileUrlManager(),
        )
      }

    SdkUtils.addSdkIfNeeded(sdk)
  }

  private suspend fun addBspFetchedAndroidSdks() {
    androidSdkGetterExtension()?.let { extension ->
      withSubtask(
        "add-bsp-fetched-android-sdks",
        BspPluginBundle.message("console.task.model.add.android.fetched.sdks"),
      ) {
        bspTracer.spanBuilder("add.bsp.fetched.android.sdks.ms").useWithScope {
          androidSdks?.forEach { addAndroidSdkIfNeeded(it, extension) }
        }
      }
    }
  }

  private suspend fun addAndroidSdkIfNeeded(androidSdk: AndroidSdk, androidSdkGetterExtension: AndroidSdkGetterExtension) {
    val sdk = writeAction { androidSdkGetterExtension.getAndroidSdk(androidSdk) } ?: return
    SdkUtils.addSdkIfNeeded(sdk)
  }

  private suspend fun applyChangesOnWorkspaceModel() =
    withSubtask(
      "apply-changes-on-workspace-model",
      BspPluginBundle.message("console.task.model.apply.changes"),
    ) {
      bspTracer.spanBuilder("apply.changes.on.workspace.model.ms").useWithScope {
        applyOnWorkspaceModel()
      }
    }

  private suspend fun applyOnWorkspaceModel() {
    val workspaceModel = WorkspaceModel.getInstance(project) as WorkspaceModelInternal
    val snapshot = workspaceModel.getBuilderSnapshot()
    bspTracer.spanBuilder("replacebysource.in.apply.on.workspace.model.ms").use {
      snapshot.builder.replaceBySource({ it.isBspRelevant() }, builder!!)
    }
    val storageReplacement = snapshot.getStorageReplacement()
    writeAction {
      val workspaceModelUpdated =
        bspTracer.spanBuilder("replaceprojectmodel.in.apply.on.workspace.model.ms").use {
          workspaceModel.replaceProjectModel(storageReplacement)
        }
      if (!workspaceModelUpdated) {
        error("Project model is not updated successfully. Try `reload` action to recalculate the project model.")
      }
    }
  }

  private fun EntitySource.isBspRelevant() =
    when (this) {
      // avoid touching global sources
      is JpsGlobalFileEntitySource -> false

      is JpsFileEntitySource,
      is JpsFileDependentEntitySource,
      is BspEntitySource,
      is BspDummyEntitySource,
      -> true

      else -> false
    }

  private suspend fun addBspFetchedJavacOptions() =
    writeAction {
      val javacOptions = JavacConfiguration.getOptions(project, JavacConfiguration::class.java)
      javacOptions.ADDITIONAL_OPTIONS_OVERRIDE = this.javacOptions
    }

  /**
   * Workaround for https://youtrack.jetbrains.com/issue/KT-70632
   */
  private suspend fun Project.refreshKotlinHighlighting() =
    writeAction {
      analysisMessageBus.syncPublisher(KotlinModificationTopics.GLOBAL_MODULE_STATE_MODIFICATION).onModification()
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
public suspend fun calculateProjectDetailsWithCapabilities(
  project: Project,
  server: JoinedBuildServer,
  buildServerCapabilities: BazelBuildServerCapabilities,
  projectRootDir: String,
  buildProject: Boolean,
): ProjectDetails =
  coroutineScope {
    val log = logger<Any>()

    suspend fun <Result> query(
      check: Boolean,
      queryName: String,
      doQuery: () -> CompletableFuture<Result>,
    ): Result? =
      try {
        if (check) {
          withContext(Dispatchers.IO) { doQuery().await() }
        } else {
          null
        }
      } catch (e: Exception) {
        when (e) {
          is CancellationException -> log.info("Query $queryName is cancelled")
          else -> log.warn("Query $queryName failed", e)
        }
        throw e
      }

    fun <Result> asyncQuery(
      check: Boolean,
      queryName: String,
      doQuery: () -> CompletableFuture<Result>,
    ): Deferred<Result?> = async { query(check, queryName, doQuery) }

    suspend fun queryBuildTargets(): WorkspaceBuildTargetsResult? =
      if (!buildProject) {
        query(true, "workspace/buildTargets") { server.workspaceBuildTargets() }
      } else {
        query(true, "workspace/buildAndGetBuildTargets") { server.workspaceBuildAndGetBuildTargets() }
      }

    try {
      val workspaceBuildTargetsResult = queryBuildTargets() ?: throw IllegalStateException("query build target is not successful")

      val allTargetsIds = calculateAllTargetsIds(workspaceBuildTargetsResult)

      val sourcesResult =
        asyncQuery(true, "buildTarget/sources") {
          server.buildTargetSources(SourcesParams(allTargetsIds))
        }

      // We have to check == true because bsp4j uses non-primitive Boolean (which is Boolean? in Kotlin)
      val resourcesResult =
        asyncQuery(buildServerCapabilities.resourcesProvider == true, "buildTarget/resources") {
          server.buildTargetResources(ResourcesParams(allTargetsIds))
        }

      val dependencySourcesResult =
        asyncQuery(buildServerCapabilities.dependencySourcesProvider == true, "buildTarget/dependencySources") {
          server.buildTargetDependencySources(DependencySourcesParams(allTargetsIds))
        }

      val javaTargetIds = calculateJavaTargetIds(workspaceBuildTargetsResult)
      val scalaTargetIds = calculateScalaTargetIds(workspaceBuildTargetsResult)
      val libraries: WorkspaceLibrariesResult? =
        query(buildServerCapabilities.workspaceLibrariesProvider, "workspace/libraries") {
          (server as BazelBuildServer).workspaceLibraries()
        }

      val directoriesResult =
        query(buildServerCapabilities.workspaceDirectoriesProvider, "workspace/directories") {
          (server as BazelBuildServer).workspaceDirectories()
        }

      val jvmBinaryJarsResult =
        query(
          BspFeatureFlags.isAndroidSupportEnabled &&
            buildServerCapabilities.jvmBinaryJarsProvider &&
            javaTargetIds.isNotEmpty(),
          "buildTarget/jvmBinaryJars",
        ) {
          server.buildTargetJvmBinaryJars(JvmBinaryJarsParams(javaTargetIds))
        }

      // We use javacOptions only to build the dependency tree based on the classpath.
      // If the workspace/libraries endpoint is NOT available (like SBT), we need to retrieve it.
      // If a server supports buildTarget/jvmCompileClasspath, then the classpath won't be passed via this endpoint
      // (see https://build-server-protocol.github.io/docs/extensions/java#javacoptionsitem).
      // In this case we can use this request to retrieve the javac options without the overhead of passing the whole classpath.
      // There's no capability for javacOptions.
      val javacOptionsResult =
        if (libraries == null || buildServerCapabilities.jvmCompileClasspathProvider) {
          asyncQuery(javaTargetIds.isNotEmpty(), "buildTarget/javacOptions") {
            server.buildTargetJavacOptions(JavacOptionsParams(javaTargetIds))
          }
        } else {
          null
        }

      // Same for Scala
      val scalacOptionsResult =
        if (libraries == null) {
          asyncQuery(scalaTargetIds.isNotEmpty() && BspFeatureFlags.isScalaSupportEnabled, "buildTarget/scalacOptions") {
            server.buildTargetScalacOptions(ScalacOptionsParams(scalaTargetIds))
          }
        } else {
          null
        }

      val pythonTargetsIds = calculatePythonTargetsIds(workspaceBuildTargetsResult)
      val pythonOptionsResult =
        asyncQuery(pythonTargetsIds.isNotEmpty() && BspFeatureFlags.isPythonSupportEnabled, "buildTarget/pythonOptions") {
          server.buildTargetPythonOptions(PythonOptionsParams(pythonTargetsIds))
        }

      val outputPathsResult =
        asyncQuery(buildServerCapabilities.outputPathsProvider == true, "buildTarget/outputPaths") {
          server.buildTargetOutputPaths(OutputPathsParams(allTargetsIds))
        }

      project.projectSyncHook?.onSync(project, server)

      ProjectDetails(
        targetIds = allTargetsIds,
        targets = workspaceBuildTargetsResult.targets.toSet(),
        sources = sourcesResult.await()?.items ?: emptyList(),
        resources = resourcesResult.await()?.items ?: emptyList(),
        dependenciesSources = dependencySourcesResult.await()?.items ?: emptyList(),
        javacOptions = javacOptionsResult?.await()?.items ?: emptyList(),
        scalacOptions = scalacOptionsResult?.await()?.items ?: emptyList(),
        pythonOptions = pythonOptionsResult.await()?.items ?: emptyList(),
        outputPathUris = outputPathsResult.await()?.obtainDistinctUris() ?: emptyList(),
        libraries = libraries?.libraries,
        directories =
          directoriesResult
            ?: WorkspaceDirectoriesResult(listOf(DirectoryItem(projectRootDir)), emptyList()),
        jvmBinaryJars = jvmBinaryJarsResult?.items ?: emptyList(),
      )
    } catch (e: Exception) {
      // TODO the type xd
      if (e is CancellationException || e is ExecutionException && e.cause is CancellationException) {
        log.debug("calculateProjectDetailsWithCapabilities has been cancelled", e)
      } else {
        log.error("calculateProjectDetailsWithCapabilities has failed", e)
      }

      throw e
    }
  }

private fun calculateAllTargetsIds(workspaceBuildTargetsResult: WorkspaceBuildTargetsResult): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.map { it.id }

private fun calculateJavaTargetIds(workspaceBuildTargetsResult: WorkspaceBuildTargetsResult): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.filter { it.languageIds.includesJava() }.map { it.id }

private fun calculateScalaTargetIds(workspaceBuildTargetsResult: WorkspaceBuildTargetsResult): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.filter { it.languageIds.includesScala() }.map { it.id }

private fun calculatePythonTargetsIds(workspaceBuildTargetsResult: WorkspaceBuildTargetsResult): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.filter { it.languageIds.includesPython() }.map { it.id }

private fun OutputPathsResult.obtainDistinctUris(): List<String> =
  this.items
    .filterNotNull()
    .flatMap { it.outputPaths }
    .map { it.uri }
    .distinct()
