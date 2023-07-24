package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.JavacOptionsResult
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.PythonOptionsResult
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.progress.indeterminateStep
import com.intellij.openapi.progress.progressStep
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.getInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.jetbrains.magicmetamodel.MagicMetaModelDiff
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.WorkspaceLibrariesResult
import org.jetbrains.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.magicmetamodel.impl.PerformanceLogger.logPerformanceSuspend
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.extractJvmBuildTarget
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.extractPythonBuildTarget
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.javaVersionToJdkName
import org.jetbrains.plugins.bsp.extension.points.PythonSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.server.client.importSubtaskId
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import kotlin.io.path.toPath

public data class PythonSdk(
  val name: String,
  val interpreter: String,
  val dependencies: List<DependencySourcesItem>
)

public class CollectProjectDetailsTask(project: Project, private val taskId: Any) :
  BspServerTask<ProjectDetails>("collect project details", project) {

  private val cancelOnFuture = CompletableFuture<Void>()

  private val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole

  private var magicMetaModelDiff: MagicMetaModelDiff? = null

  private lateinit var uniqueJdkInfos: Set<JvmBuildTarget>

  private var pythonSdks: Set<PythonSdk>? = null

  private val jdkTable = ProjectJdkTable.getInstance()

  private lateinit var coroutineJob: Job

  public suspend fun execute(name: String, cancelable: Boolean) {
    withContext(Dispatchers.Default) {
      coroutineJob = launch {
        try {
          withBackgroundProgress(project, name, cancelable) {
            doExecute()
          }
        } catch (e: CancellationException) {
          onCancel(e)
        }
      }
      coroutineJob.join()
    }
  }

  private suspend fun doExecute() {
    val projectDetails = progressStep(endFraction = 0.5, text = "Collecting project details") {
      runInterruptible { calculateProjectDetailsSubtask() }
    }
    indeterminateStep(text = "Calculating all unique jdk infos") {
      calculateAllUniqueJdkInfosSubtask(projectDetails)
    }
    if (pythonSdkGetterExtensionExists()) {
      indeterminateStep(text = "Calculating all unique python sdk infos") {
        calculateAllPythonSdkInfosSubtask(projectDetails)
      }
    }
    progressStep(endFraction = 0.75, "Updating magic meta model diff") {
      runInterruptible { updateMMMDiffSubtask(projectDetails) }
    }
    progressStep(endFraction = 1.0, "Post-processing magic meta model") {
      postprocessingMMMSubtask()
    }
  }

  private fun calculateProjectDetailsSubtask() =
    logPerformance("collect-project-details") {
      connectAndExecuteWithServer { server, capabilities -> collectModel(server, capabilities, cancelOnFuture) }
    }

  private fun collectModel(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    cancelOn: CompletableFuture<Void>
  ): ProjectDetails? {
    fun isCancellationException(e: Throwable): Boolean =
      e is CompletionException && e.cause is CancellationException

    fun isTimeoutException(e: Throwable): Boolean =
      e is CompletionException && e.cause is TimeoutException

    fun errorCallback(e: Throwable) = when {
      isCancellationException(e) ->
        bspSyncConsole.finishTask(
          taskId = taskId,
          message = "Canceled",
          result = FailureResultImpl("The task is canceled")
        )

      isTimeoutException(e) ->
        bspSyncConsole.finishTask(
          taskId = taskId,
          message = "Timed out",
          result = FailureResultImpl(BspTasksBundle.message("task.timeout.message"))
        )

      else -> bspSyncConsole.finishTask(taskId, "Failed", FailureResultImpl(e))
    }

    bspSyncConsole.startSubtask(taskId, importSubtaskId, "Collecting model...")

    val projectDetails =
      calculateProjectDetailsWithCapabilities(
        server = server,
        buildServerCapabilities = capabilities,
        errorCallback = { errorCallback(it) },
        cancelOn = cancelOn
      )

    bspSyncConsole.finishSubtask(importSubtaskId, "Collecting model done!")

    return projectDetails
  }

  private fun calculateAllUniqueJdkInfosSubtask(projectDetails: ProjectDetails?) {
    projectDetails?.let {
      bspSyncConsole.startSubtask(taskId, "calculate-all-unique-jdk-infos", "Calculating all unique jdk infos...")
      uniqueJdkInfos = logPerformance("calculate-all-unique-jdk-infos") { calculateAllUniqueJdkInfos(projectDetails) }
      bspSyncConsole.finishSubtask("calculate-all-unique-jdk-infos", "Calculating all unique jdk infos done!")
    }
  }

  private fun calculateAllUniqueJdkInfos(projectDetails: ProjectDetails): Set<JvmBuildTarget> =
    projectDetails.targets.mapNotNull(::extractJvmBuildTarget).toSet()

  private fun calculateAllPythonSdkInfosSubtask(projectDetails: ProjectDetails?) {
    projectDetails?.let {
      bspSyncConsole.startSubtask(taskId, "calculate-all-python-sdk-infos", "Calculating all python sdk infos...")
      pythonSdks = logPerformance("calculate-all-python-sdk-infos") { calculateAllPythonSdkInfos(projectDetails) }
      bspSyncConsole.finishSubtask("calculate-all-python-sdk-infos", "Calculating all python sdk infos done!")
    }
  }

  private fun createPythonSdk(target: BuildTarget, dependenciesSources: List<DependencySourcesItem>): PythonSdk? =
      extractPythonBuildTarget(target)
        ?.takeIf { it.version != null && it.interpreter != null }
        ?.let {
          PythonSdk(
            name = "${target.id.uri}-${it.version}",
            interpreter = it.interpreter,
            dependencies = dependenciesSources
          )
        }

  private fun calculateAllPythonSdkInfos(projectDetails: ProjectDetails): Set<PythonSdk> {
    return projectDetails.targets
      .mapNotNull {
        createPythonSdk(it, projectDetails.dependenciesSources.filter { a -> a.target.uri == it.id.uri })
      }
      .toSet()
  }

  private fun updateMMMDiffSubtask(projectDetails: ProjectDetails?) {
    val magicMetaModelService = MagicMetaModelService.getInstance(project)
    bspSyncConsole.startSubtask(taskId, "calculate-project-structure", "Calculating project structure...")
    projectDetails?.let {
      logPerformance("initialize-magic-meta-model") { magicMetaModelService.initializeMagicModel(projectDetails) }
    }
    magicMetaModelDiff = logPerformance("load-default-targets") { magicMetaModelService.value.loadDefaultTargets() }
    bspSyncConsole.finishSubtask("calculate-project-structure", "Calculating project structure done!")
  }

  private suspend fun postprocessingMMMSubtask() {
    addBspFetchedJdks()
    applyChangesOnWorkspaceModel()
    addBspFetchedPythonSdks()
  }

  private suspend fun addBspFetchedJdks() {
    bspSyncConsole.startSubtask(taskId, "add-bsp-fetched-jdks", "Adding BSP-fetched JDKs...")
    logPerformanceSuspend("add-bsp-fetched-jdks") { uniqueJdkInfos.forEach { addJdk(it) } }
    bspSyncConsole.finishSubtask("add-bsp-fetched-jdks", "Adding BSP-fetched JDKs done!")
  }

  private suspend fun addJdk(jdkInfo: JvmBuildTarget) {
    val jdk = ExternalSystemJdkProvider.getInstance().createJdk(
      jdkInfo.javaVersion.javaVersionToJdkName(project.name),
      URI.create(jdkInfo.javaHome).toPath().toString()
    )

    addJdkIfNeeded(jdk)
  }

  private suspend fun addJdkIfNeeded(jdk: Sdk) {
    val existingJdk = jdkTable.findJdk(jdk.name, jdk.sdkType.name)
    if (existingJdk == null || existingJdk.homePath != jdk.homePath) {
      writeAction {
        existingJdk?.let { jdkTable.removeJdk(existingJdk) }
        jdkTable.addJdk(jdk)
      }
    }
  }

  private suspend fun addBspFetchedPythonSdks() {
    pythonSdkGetterExtension()?.let { extension ->
      bspSyncConsole.startSubtask(taskId, "add-bsp-fetched-python-sdks", "Adding BSP-fetched Python SDKs...")
      logPerformanceSuspend("add-bsp-fetched-python-sdks") {
        pythonSdks?.forEach { addPythonSdkIfNeeded(it, extension) }
      }
      bspSyncConsole.finishSubtask("add-bsp-fetched-python-sdks", "Adding BSP-fetched Python SDKs done!")
    }
  }

  private suspend fun addPythonSdkIfNeeded(pythonSdk: PythonSdk, pythonSdkGetterExtension: PythonSdkGetterExtension) {
    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)
    val sdk = pythonSdkGetterExtension.getPythonSdk(pythonSdk, jdkTable, virtualFileUrlManager)

    addJdkIfNeeded(sdk)
  }

  private suspend fun applyChangesOnWorkspaceModel() {
    bspSyncConsole.startSubtask(taskId, "apply-on-workspace-model", "Applying changes...")

    logPerformanceSuspend("apply-changes-on-workspace-model") {
      magicMetaModelDiff?.applyOnWorkspaceModel()
    }

    bspSyncConsole.finishSubtask("apply-on-workspace-model", "Applying changes done!")
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

@Suppress("LongMethod")
public fun calculateProjectDetailsWithCapabilities(
  server: BspServer,
  buildServerCapabilities: BuildServerCapabilities,
  errorCallback: (Throwable) -> Unit,
  cancelOn: CompletableFuture<Void> = CompletableFuture(),
): ProjectDetails? {
  val log = logger<Any>()

  try {
    val workspaceBuildTargetsResult = queryForBuildTargets(server)
      .reactToExceptionIn(cancelOn)
      .catchSyncErrors(errorCallback)
      .get()

    val allTargetsIds = calculateAllTargetsIds(workspaceBuildTargetsResult)

    val sourcesFuture = queryForSourcesResult(server, allTargetsIds)
      .reactToExceptionIn(cancelOn)
      .catchSyncErrors(errorCallback)

    val resourcesFuture =
      queryForTargetResources(server, buildServerCapabilities, allTargetsIds)
        ?.reactToExceptionIn(cancelOn)
        ?.catchSyncErrors(errorCallback)
    val dependencySourcesFuture =
      queryForDependencySources(server, buildServerCapabilities, allTargetsIds)
        ?.reactToExceptionIn(cancelOn)
        ?.catchSyncErrors(errorCallback)

    val javaTargetsIds = calculateJavaTargetsIds(workspaceBuildTargetsResult)
    val libraries: WorkspaceLibrariesResult? = server.workspaceLibraries()
      .reactToExceptionIn(cancelOn)
      .exceptionally {
        log.warn(it)
        null
      }
      .get()

    // We use javacOptions only do build dependency tree based on classpath
    // If workspace/libraries endpoint is available (like in bazel-bsp)
    // we don't need javacOptions at all. For other servers (like SBT)
    // we still need to retrieve it
    val javacOptionsFuture = if (libraries == null)
      queryForJavacOptions(server, javaTargetsIds)
        ?.reactToExceptionIn(cancelOn)
        ?.catchSyncErrors(errorCallback)
    else null

    val pythonTargetsIds = calculatePythonTargetsIds(workspaceBuildTargetsResult)
    val pythonOptionsFuture = queryForPythonOptions(server, pythonTargetsIds)
      ?.reactToExceptionIn(cancelOn)
      ?.catchSyncErrors(errorCallback)

    val outputPathsFuture =
      queryForOutputPaths(server, allTargetsIds)
              .reactToExceptionIn(cancelOn)
              .catchSyncErrors(errorCallback)
    return ProjectDetails(
      targetsId = allTargetsIds,
      targets = workspaceBuildTargetsResult.targets.toSet(),
      sources = sourcesFuture.get().items,
      resources = resourcesFuture?.get()?.items ?: emptyList(),
      dependenciesSources = dependencySourcesFuture?.get()?.items ?: emptyList(),
      javacOptions = javacOptionsFuture?.get()?.items ?: emptyList(),
      pythonOptions = pythonOptionsFuture?.get()?.items ?: emptyList(),
      outputPathUris = outputPathsFuture.get().obtainDistinctUris(),
      libraries = libraries?.libraries,
    )
  } catch (e: Exception) {
    // TODO the type xd
    if (e is ExecutionException && e.cause is CancellationException) {
      log.debug("calculateProjectDetailsWithCapabilities has been cancelled!", e)
    } else {
      log.error("calculateProjectDetailsWithCapabilities has failed!", e)
    }

    return null
  }
}

private fun queryForBuildTargets(server: BspServer): CompletableFuture<WorkspaceBuildTargetsResult> =
  server.workspaceBuildTargets()

private fun calculateAllTargetsIds(
  workspaceBuildTargetsResult: WorkspaceBuildTargetsResult
): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.map { it.id }

private fun queryForSourcesResult(
  server: BspServer,
  allTargetsIds: List<BuildTargetIdentifier>
): CompletableFuture<SourcesResult> {
  val sourcesParams = SourcesParams(allTargetsIds)

  return server.buildTargetSources(sourcesParams)
}

private fun queryForTargetResources(
  server: BspServer,
  capabilities: BuildServerCapabilities,
  allTargetsIds: List<BuildTargetIdentifier>
): CompletableFuture<ResourcesResult>? {
  val resourcesParams = ResourcesParams(allTargetsIds)

  return if (capabilities.resourcesProvider) server.buildTargetResources(resourcesParams)
  else null
}

private fun queryForDependencySources(
  server: BspServer,
  capabilities: BuildServerCapabilities,
  allTargetsIds: List<BuildTargetIdentifier>
): CompletableFuture<DependencySourcesResult>? {
  val dependencySourcesParams = DependencySourcesParams(allTargetsIds)

  return if (capabilities.dependencySourcesProvider) server.buildTargetDependencySources(dependencySourcesParams)
  else null
}

private fun calculateJavaTargetsIds(
  workspaceBuildTargetsResult: WorkspaceBuildTargetsResult): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.filter { it.languageIds.contains("java") }.map { it.id }

private fun queryForJavacOptions(
  server: BspServer,
  javaTargetsIds: List<BuildTargetIdentifier>
): CompletableFuture<JavacOptionsResult>? {
  return if (javaTargetsIds.isNotEmpty()) {
    val javacOptionsParams = JavacOptionsParams(javaTargetsIds)
    server.buildTargetJavacOptions(javacOptionsParams)
  } else null
}

private fun calculatePythonTargetsIds(
    workspaceBuildTargetsResult: WorkspaceBuildTargetsResult): List<BuildTargetIdentifier> =
  workspaceBuildTargetsResult.targets.filter { it.languageIds.contains("python") }.map { it.id }

private fun queryForPythonOptions(
  server: BspServer,
  pythonTargetsIds: List<BuildTargetIdentifier>
): CompletableFuture<PythonOptionsResult>? {
  return if (pythonTargetsIds.isNotEmpty()) {
    val pythonOptionsParams = PythonOptionsParams(pythonTargetsIds)
    server.buildTargetPythonOptions(pythonOptionsParams)
  } else null
}

private fun queryForOutputPaths(
  server: BspServer,
  allTargetIds: List<BuildTargetIdentifier>
): CompletableFuture<OutputPathsResult> {
  val outputPathsParams = OutputPathsParams(allTargetIds)
  return server.buildTargetOutputPaths(outputPathsParams)
}

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
