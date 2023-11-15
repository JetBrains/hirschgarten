package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.indeterminateStep
import com.intellij.platform.util.progress.progressStep
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.getInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.jetbrains.bsp.BazelBuildServerCapabilities
import org.jetbrains.bsp.DirectoryItem
import org.jetbrains.bsp.WorkspaceDirectoriesResult
import org.jetbrains.bsp.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.WorkspaceLibrariesResult
import org.jetbrains.bsp.utils.extractJvmBuildTarget
import org.jetbrains.bsp.utils.extractPythonBuildTarget
import org.jetbrains.bsp.utils.extractScalaBuildTarget
import org.jetbrains.magicmetamodel.MagicMetaModelDiff
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.BenchmarkFlags.isBenchmark
import org.jetbrains.magicmetamodel.impl.PerformanceLogger
import org.jetbrains.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.magicmetamodel.impl.PerformanceLogger.logPerformanceSuspend
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.javaVersionToJdkName
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesJava
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesPython
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesScala
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.PythonSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.extension.points.scalaSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.scalaSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.server.client.importSubtaskId
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import kotlin.io.path.Path
import kotlin.io.path.toPath
import kotlin.system.exitProcess

public data class PythonSdk(
  val name: String,
  val interpreterUri: String,
  val dependencies: List<DependencySourcesItem>,
)

public data class ScalaSdk(
  val name: String,
  val scalaVersion: String,
  val sdkJars: List<String>,
)

public class CollectProjectDetailsTask(project: Project, private val taskId: Any) :
  BspServerTask<ProjectDetails>("collect project details", project) {
  private val cancelOnFuture = CompletableFuture<Void>()

  private val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole

  private var magicMetaModelDiff: MagicMetaModelDiff? = null

  private var uniqueJdkInfos: Set<JvmBuildTarget>? = null

  private var pythonSdks: Set<PythonSdk>? = null

  private var scalaSdks: Set<ScalaSdk>? = null

  private val sdkTable = ProjectJdkTable.getInstance()

  private lateinit var coroutineJob: Job

  public suspend fun execute(name: String, cancelable: Boolean) {
    saveAllFiles()
    withContext(Dispatchers.Default) {
      coroutineJob = launch {
        try {
          withBackgroundProgress(project, name, cancelable) {
            doExecute()
          }
          PerformanceLogger.dumpMetrics()
        } catch (e: CancellationException) {
          onCancel(e)
        } finally {
          if (isBenchmark()) {
            exitProcess(0)
          }
        }
      }
      coroutineJob.join()
    }
  }

  private suspend fun doExecute() {
    val projectDetails = progressStep(
      endFraction = 0.5,
      text = BspPluginBundle.message("progress.bar.collect.project.details")
    ) {
      runInterruptible { calculateProjectDetailsSubtask() }
    } ?: return
    indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.jdk.infos")) {
      calculateAllUniqueJdkInfosSubtask(projectDetails)
    }
    if (BspFeatureFlags.isPythonSupportEnabled && pythonSdkGetterExtensionExists()) {
      indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.python.sdk.infos")) {
        calculateAllPythonSdkInfosSubtask(projectDetails)
      }
    }
    if (BspFeatureFlags.isScalaSupportEnabled && scalaSdkGetterExtensionExists()) {
      indeterminateStep(text = "Calculating all unique scala sdk infos") {
        calculateAllScalaSdkInfosSubtask(projectDetails)
      }
    }
    progressStep(endFraction = 0.75, BspPluginBundle.message("progress.bar.update.mmm.diff")) {
      updateMMMDiffSubtask(projectDetails)
    }
    progressStep(endFraction = 1.0, BspPluginBundle.message("progress.bar.post.processing.mmm")) {
      postprocessingMMMSubtask()
    }
  }

  private fun calculateProjectDetailsSubtask() =
    logPerformance("collect-project-details") {
      connectAndExecuteWithServer { server, capabilities -> collectModel(server, capabilities, cancelOnFuture) }
    }

  private fun collectModel(
    server: BspServer,
    capabilities: BazelBuildServerCapabilities,
    cancelOn: CompletableFuture<Void>,
  ): ProjectDetails? {
    fun isCancellationException(e: Throwable): Boolean =
      e is CompletionException && e.cause is CancellationException

    fun isTimeoutException(e: Throwable): Boolean =
      e is CompletionException && e.cause is TimeoutException

    fun errorCallback(e: Throwable) = when {
      isCancellationException(e) ->
        bspSyncConsole.finishTask(
          taskId = taskId,
          message = BspPluginBundle.message("console.task.exception.cancellation"),
          result = FailureResultImpl(BspPluginBundle.message("console.task.exception.cancellation.message")),
        )

      isTimeoutException(e) ->
        bspSyncConsole.finishTask(
          taskId = taskId,
          message = BspPluginBundle.message("console.task.exception.timed.out"),
          result = FailureResultImpl(BspPluginBundle.message("console.task.exception.timeout.message")),
        )

      else -> bspSyncConsole.finishTask(
        taskId,
        BspPluginBundle.message("console.task.exception.other"), FailureResultImpl(e)
      )
    }

    bspSyncConsole.startSubtask(
      this.taskId, importSubtaskId,
      BspPluginBundle.message("console.task.model.collect.in.progress")
    )

    val projectDetails =
      calculateProjectDetailsWithCapabilities(
        server = server,
        buildServerCapabilities = capabilities,
        projectRootDir = project.rootDir.url,
        errorCallback = { errorCallback(it) },
        cancelOn = cancelOn,
      )

    bspSyncConsole.finishSubtask(importSubtaskId, BspPluginBundle.message("console.task.model.collect.success"))

    return projectDetails
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

  private suspend fun calculateAllUniqueJdkInfosSubtask(projectDetails: ProjectDetails) = withSubtask(
    "calculate-all-unique-jdk-infos",
    BspPluginBundle.message("console.task.model.calculate.jdks.infos")
  ) {
    uniqueJdkInfos = logPerformance(it) {
      calculateAllUniqueJdkInfos(projectDetails)
    }
  }

  private fun calculateAllUniqueJdkInfos(projectDetails: ProjectDetails): Set<JvmBuildTarget> =
    projectDetails.targets.mapNotNull(::extractJvmBuildTarget).toSet()

  private suspend fun calculateAllScalaSdkInfosSubtask(projectDetails: ProjectDetails) = withSubtask(
    "calculate-all-scala-sdk-infos",
    BspPluginBundle.message("console.task.model.calculate.scala.sdk.infos")
  ) {
    scalaSdks = logPerformance(it) {
      calculateAllScalaSdkInfos(projectDetails)
    }
  }

  private fun calculateAllScalaSdkInfos(projectDetails: ProjectDetails): Set<ScalaSdk> =
    projectDetails.targets
      .mapNotNull {
        createScalaSdk(it)
      }
      .toSet()

  private fun createScalaSdk(target: BuildTarget): ScalaSdk? =
    extractScalaBuildTarget(target)
      ?.let {
        ScalaSdk(
          name = "${target.id.uri}-${it.scalaVersion}",
          scalaVersion = it.scalaVersion,
          sdkJars = it.jars
        )
      }

  private suspend fun calculateAllPythonSdkInfosSubtask(projectDetails: ProjectDetails) = withSubtask(
    "calculate-all-python-sdk-infos",
    BspPluginBundle.message("console.task.model.calculate.python.sdks.done")
  ) {
    runInterruptible {
      pythonSdks = logPerformance(it) {
        calculateAllPythonSdkInfos(projectDetails)
      }
    }
  }

  private fun createPythonSdk(target: BuildTarget, dependenciesSources: List<DependencySourcesItem>): PythonSdk? =
    extractPythonBuildTarget(target)?.let {
      if (it.interpreter != null && it.version != null)
        PythonSdk(
          name = "${target.id.uri}-${it.version}",
          interpreterUri = it.interpreter,
          dependencies = dependenciesSources,
        )
      else
        pythonSdkGetterExtension()
          ?.getSystemSdk()
          ?.let { sdk ->
            PythonSdk(
              name = "${target.id.uri}-detected-PY3",
              interpreterUri = Path(sdk.homePath!!).toUri().toString(),
              dependencies = dependenciesSources
            )
          }
    }

  private fun calculateAllPythonSdkInfos(projectDetails: ProjectDetails): Set<PythonSdk> {
    return projectDetails.targets.mapNotNull {
      createPythonSdk(it, projectDetails.dependenciesSources.filter { a -> a.target.uri == it.id.uri })
    }
      .toSet()
  }

  private suspend fun updateMMMDiffSubtask(projectDetails: ProjectDetails) {
    val magicMetaModelService = MagicMetaModelService.getInstance(project)
    withSubtask("calculate-project-structure", BspPluginBundle.message("console.task.model.calculate.structure")) {
      runInterruptible {
        logPerformance("initialize-magic-meta-model") {
          magicMetaModelService.initializeMagicModel(projectDetails)
        }
        magicMetaModelDiff = logPerformance("load-default-targets") {
          magicMetaModelService.value.loadDefaultTargets()
        }
      }
    }
  }

  private suspend fun postprocessingMMMSubtask() {
    addBspFetchedJdks()
    applyChangesOnWorkspaceModel()

    if (BspFeatureFlags.isPythonSupportEnabled) {
      addBspFetchedPythonSdks()
    }

    if (BspFeatureFlags.isScalaSupportEnabled) {
      addBspFetchedScalaSdks()
    }
  }

  private suspend fun addBspFetchedJdks() = withSubtask(
    "add-bsp-fetched-jdks",
    BspPluginBundle.message("console.task.model.add.fetched.jdks")
  ) {
    logPerformanceSuspend("add-bsp-fetched-jdks") { uniqueJdkInfos?.forEach { addJdk(it) } }
  }

  private suspend fun addJdk(jdkInfo: JvmBuildTarget) {
    val jdk = ExternalSystemJdkProvider.getInstance().createJdk(
      jdkInfo.javaVersion.javaVersionToJdkName(project.name),
      URI.create(jdkInfo.javaHome).toPath().toString(),
    )

    addSdkIfNeeded(jdk)
  }

  private suspend fun addSdkIfNeeded(sdk: Sdk) {
    val existingSdk = sdkTable.findJdk(sdk.name, sdk.sdkType.name)
    if (existingSdk == null || existingSdk.homePath != sdk.homePath) {
      writeAction {
        existingSdk?.let { sdkTable.removeJdk(existingSdk) }
        sdkTable.addJdk(sdk)
      }
    }
  }

  private suspend fun addBspFetchedScalaSdks() {
    scalaSdkGetterExtension()?.let { extension ->
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
        BspPluginBundle.message("console.task.model.add.python.fetched.sdks")
      ) {
        logPerformanceSuspend("add-bsp-fetched-python-sdks") {
          pythonSdks?.forEach { addPythonSdkIfNeeded(it, extension) }
        }
      }
    }
  }

  private suspend fun addPythonSdkIfNeeded(pythonSdk: PythonSdk, pythonSdkGetterExtension: PythonSdkGetterExtension) {
    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)
    val sdk = runInterruptible { pythonSdkGetterExtension.getPythonSdk(pythonSdk, virtualFileUrlManager) }

    addSdkIfNeeded(sdk)
  }

  private suspend fun applyChangesOnWorkspaceModel() = withSubtask(
    "apply-changes-on-workspace-model",
    BspPluginBundle.message("console.task.model.apply.changes")
  ) {
    logPerformanceSuspend("apply-changes-on-workspace-model") {
      magicMetaModelDiff?.applyOnWorkspaceModel()
    }
  }

  private fun onCancel(e: Exception) {
    cancelOnFuture.cancel(true)
    bspSyncConsole.finishTask(taskId, e.message ?: "", FailureResultImpl(e))
  }

  public fun cancelExecution() {
    cancelOnFuture.cancel(true)
    coroutineJob.cancel()
  }
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
public fun calculateProjectDetailsWithCapabilities(
  server: BspServer,
  buildServerCapabilities: BazelBuildServerCapabilities,
  projectRootDir: String,
  errorCallback: (Throwable) -> Unit,
  cancelOn: CompletableFuture<Void> = CompletableFuture(),
): ProjectDetails? {
  val log = logger<Any>()

  fun <Result> query(
    check: Boolean,
    queryName: String,
    doQuery: () -> CompletableFuture<Result>,
  ): CompletableFuture<Result>? =
    if (check) doQuery()
      .reactToExceptionIn(cancelOn)
      .catchSyncErrors(errorCallback)
      .exceptionally {
        log.warn("Query '$queryName' has failed", it)
        null
      }
    else null

  try {
    val workspaceBuildTargetsResult = query(true, "workspace/buildTargets") { server.workspaceBuildTargets() }!!
      .get()

    val allTargetsIds = calculateAllTargetsIds(workspaceBuildTargetsResult)

    val sourcesFuture = query(true, "buildTarget/sources") {
      server.buildTargetSources(SourcesParams(allTargetsIds))
    }!!

    // We have to check == true because bsp4j uses non-primitive Boolean (which is Boolean? in Kotlin)
    val resourcesFuture = query(buildServerCapabilities.resourcesProvider == true, "buildTarget/resources") {
      server.buildTargetResources(ResourcesParams(allTargetsIds))
    }

    val dependencySourcesFuture =
      query(buildServerCapabilities.dependencySourcesProvider == true, "buildTarget/dependencySources") {
        server.buildTargetDependencySources(DependencySourcesParams(allTargetsIds))
      }

    val javaTargetIds = calculateJavaTargetIds(workspaceBuildTargetsResult)
    val scalaTargetIds = calculateScalaTargetIds(workspaceBuildTargetsResult)
    val libraries: WorkspaceLibrariesResult? =
      query(buildServerCapabilities.workspaceLibrariesProvider, "workspace/libraries") {
        server.workspaceLibraries()
      }?.get()

    val directoriesFuture = query(buildServerCapabilities.workspaceDirectoriesProvider, "workspace/directories") {
      server.workspaceDirectories()
    }

    val invalidTargetsFuture =
      query(buildServerCapabilities.workspaceInvalidTargetsProvider, "workspace/invalidTargets") {
        server.workspaceInvalidTargets()
      }

    // We use javacOptions only do build dependency tree based on classpath
    // If workspace/libraries endpoint is available (like in bazel-bsp)
    // we don't need javacOptions at all. For other servers (like SBT)
    // we still need to retrieve it
    // There's no capability for javacOptions
    val javacOptionsFuture = if (libraries == null)
      query(javaTargetIds.isNotEmpty(), "buildTarget/javacOptions") {
        server.buildTargetJavacOptions(JavacOptionsParams(javaTargetIds))
      }
    else null

    // Same for Scala
    val scalacOptionsFuture = if (libraries == null)
      query(scalaTargetIds.isNotEmpty() && BspFeatureFlags.isScalaSupportEnabled, "buildTarget/scalacOptions") {
        server.buildTargetScalacOptions(ScalacOptionsParams(scalaTargetIds))
      }
    else null

    val pythonTargetsIds = calculatePythonTargetsIds(workspaceBuildTargetsResult)
    val pythonOptionsFuture =
      query(pythonTargetsIds.isNotEmpty() && BspFeatureFlags.isPythonSupportEnabled, "buildTarget/pythonOptions") {
        server.buildTargetPythonOptions(PythonOptionsParams(pythonTargetsIds))
      }

    val outputPathsFuture = query(buildServerCapabilities.outputPathsProvider == true, "buildTarget/outputPaths") {
      server.buildTargetOutputPaths(OutputPathsParams(allTargetsIds))
    }

    val invalidTargets = invalidTargetsFuture?.get() ?: WorkspaceInvalidTargetsResult(emptyList())
    if (invalidTargets.targets.isNotEmpty()) {
      BspBalloonNotifier.warn(
        BspPluginBundle.message("widget.collect.targets.not.imported.properly.title"),
        BspPluginBundle.message("widget.collect.targets.not.imported.properly.message")
      )
    }

    return ProjectDetails(
      targetsId = allTargetsIds,
      targets = workspaceBuildTargetsResult.targets.toSet(),
      sources = sourcesFuture.get().items,
      resources = resourcesFuture?.get()?.items ?: emptyList(),
      dependenciesSources = dependencySourcesFuture?.get()?.items ?: emptyList(),
      javacOptions = javacOptionsFuture?.get()?.items ?: emptyList(),
      scalacOptions = scalacOptionsFuture?.get()?.items ?: emptyList(),
      pythonOptions = pythonOptionsFuture?.get()?.items ?: emptyList(),
      outputPathUris = outputPathsFuture?.get()?.obtainDistinctUris() ?: emptyList(),
      libraries = libraries?.libraries,
      directories = directoriesFuture?.get()
        ?: WorkspaceDirectoriesResult(listOf(DirectoryItem(projectRootDir)), emptyList()),
      invalidTargets = invalidTargets,
    )
  } catch (e: Exception) {
    // TODO the type xd
    if (e is ExecutionException && e.cause is CancellationException) {
      log.debug("calculateProjectDetailsWithCapabilities has been cancelled", e)
    } else {
      log.error("calculateProjectDetailsWithCapabilities has failed", e)
    }

    return null
  }
}

private fun calculateAllTargetsIds(
  workspaceBuildTargetsResult: WorkspaceBuildTargetsResult,
): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.map { it.id }

private fun calculateJavaTargetIds(
  workspaceBuildTargetsResult: WorkspaceBuildTargetsResult,
): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.filter { it.languageIds.includesJava() }.map { it.id }

private fun calculateScalaTargetIds(
  workspaceBuildTargetsResult: WorkspaceBuildTargetsResult,
): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.filter { it.languageIds.includesScala() }.map { it.id }

private fun calculatePythonTargetsIds(
  workspaceBuildTargetsResult: WorkspaceBuildTargetsResult,
): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.filter { it.languageIds.includesPython() }.map { it.id }

private fun <T> CompletableFuture<T>.catchSyncErrors(errorCallback: (Throwable) -> Unit): CompletableFuture<T> =
  this.whenComplete { _, exception ->
    exception?.let { errorCallback(it) }
  }

private fun OutputPathsResult.obtainDistinctUris(): List<String> =
  this.items
    .filterNotNull()
    .flatMap { it.outputPaths }
    .map { it.uri }
    .distinct()
