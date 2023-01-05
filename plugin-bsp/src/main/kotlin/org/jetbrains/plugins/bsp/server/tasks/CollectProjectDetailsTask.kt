package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.InitializeBuildResult
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.JavacOptionsResult
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.google.gson.JsonObject
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelDiff
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.plugins.bsp.config.ProjectPropertiesService
import org.jetbrains.plugins.bsp.server.client.importSubtaskId
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import java.util.concurrent.CompletableFuture

public class UpdateMagicMetaModelInTheBackgroundTask(
  private val project: Project,
  private val taskId: Any,
  private val collect: () -> ProjectDetails?,
) {

  public fun executeInTheBackground(
    name: String,
    cancelable: Boolean,
    beforeRun: () -> Unit = {},
    afterOnSuccess: () -> Unit = {}
  ) {
    prepareBackgroundTask(name, cancelable, beforeRun, afterOnSuccess).queue()
  }

  private fun prepareBackgroundTask(
    name: String,
    cancelable: Boolean,
    beforeRun: () -> Unit = {},
    afterOnSuccess: () -> Unit = {}
  ) = object : Task.Backgroundable(project, name, cancelable) {

    private var magicMetaModelDiff: MagicMetaModelDiff? = null

    override fun run(indicator: ProgressIndicator) {
      beforeRun()
      updateMagicMetaModelDiff()
    }

    private fun updateMagicMetaModelDiff() {
      val magicMetaModel = logPerformance("update-magic-meta-model-diff") { initializeMagicMetaModel() }
      magicMetaModelDiff = logPerformance("load-default-targets") { magicMetaModel.loadDefaultTargets() }
    }

    // TODO ugh, it should be nicer
    private fun initializeMagicMetaModel(): MagicMetaModel {
      val magicMetaModelService = MagicMetaModelService.getInstance(project)
      val projectDetails = logPerformance("collect-project-details") { collect() }

      if (projectDetails != null) {
        logPerformance("initialize-magic-meta-model") { magicMetaModelService.initializeMagicModel(projectDetails) }
      }

      return magicMetaModelService.value
    }

    override fun onSuccess() {
      applyChangesOnWorkspaceModel()
      afterOnSuccess()
    }

    private fun applyChangesOnWorkspaceModel() {
      val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
      bspSyncConsole.startSubtask(taskId, "apply-on-workspace-model", "Updating model...")
      runWriteAction { magicMetaModelDiff?.applyOnWorkspaceModel() }
      bspSyncConsole.finishSubtask("apply-on-workspace-model", "Updating model done!")
    }
  }
}

public class CollectProjectDetailsTask(project: Project, private val taskId: Any) :
  BspServerTask<ProjectDetails>("collect project details", project) {

  private val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole

  public fun prepareBackgroundTask(): UpdateMagicMetaModelInTheBackgroundTask =
    UpdateMagicMetaModelInTheBackgroundTask(project, taskId) {
      executeWithServerIfConnected { collectModel(it) }
    }

  private fun collectModel(server: BspServer): ProjectDetails {
    fun errorCallback(e: Throwable) {
      bspSyncConsole.finishTask(taskId, "Failed", FailureResultImpl(e))
    }

    bspSyncConsole.startSubtask(taskId, importSubtaskId, "Collecting model...")

    val initializeBuildResult = queryForInitialize(server).catchSyncErrors { errorCallback(it) }.get()
    server.onBuildInitialized()


    val projectDetails =
      calculateProjectDetailsWithCapabilities(server, initializeBuildResult.capabilities) { errorCallback(it) }

    bspSyncConsole.finishSubtask(importSubtaskId, "Collection model done!")

    return projectDetails
  }

  private fun queryForInitialize(server: BspServer): CompletableFuture<InitializeBuildResult> {
    val buildParams = createInitializeBuildParams()

    return server.buildInitialize(buildParams)
  }

  private fun createInitializeBuildParams(): InitializeBuildParams {
    val projectProperties = ProjectPropertiesService.getInstance(project).value
    val projectBaseDir = projectProperties.projectRootDir
    val params = InitializeBuildParams(
      "IntelliJ-BSP",
      "0.0.1",
      "2.0.0",
      projectBaseDir.toString(),
      BuildClientCapabilities(listOf("java"))
    )
    val dataJson = JsonObject()
    dataJson.addProperty("clientClassesRootDir", "$projectBaseDir/out")
    params.data = dataJson

    return params
  }
}

public fun calculateProjectDetailsWithCapabilities(
  server: BspServer,
  buildServerCapabilities: BuildServerCapabilities,
  errorCallback: (Throwable) -> Unit
): ProjectDetails {
  val workspaceBuildTargetsResult = queryForBuildTargets(server).get()

  val allTargetsIds = calculateAllTargetsIds(workspaceBuildTargetsResult)

  val sourcesFuture = queryForSourcesResult(server, allTargetsIds).catchSyncErrors(errorCallback)
  val resourcesFuture =
    queryForTargetResources(server, buildServerCapabilities, allTargetsIds)?.catchSyncErrors(errorCallback)
  val dependencySourcesFuture =
    queryForDependencySources(server, buildServerCapabilities, allTargetsIds)?.catchSyncErrors(errorCallback)
  val javacOptionsFuture = queryForJavacOptions(server, allTargetsIds).catchSyncErrors(errorCallback)

  return ProjectDetails(
    targetsId = allTargetsIds,
    targets = workspaceBuildTargetsResult.targets.toSet(),
    sources = sourcesFuture.get().items,
    resources = resourcesFuture?.get()?.items ?: emptyList(),
    dependenciesSources = dependencySourcesFuture?.get()?.items ?: emptyList(),
    // SBT seems not to support the javacOptions endpoint and seems just to hang when called,
    // so it's just safer to add timeout here. This should not be called at all for SBT.
    javacOptions = javacOptionsFuture.get()?.items ?: emptyList()
  )
}


private fun queryForBuildTargets(server: BspServer): CompletableFuture<WorkspaceBuildTargetsResult> =
  server.workspaceBuildTargets()

private fun calculateAllTargetsIds(workspaceBuildTargetsResult: WorkspaceBuildTargetsResult): List<BuildTargetIdentifier> =
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

private fun queryForJavacOptions(
  server: BspServer,
  allTargetsIds: List<BuildTargetIdentifier>
): CompletableFuture<JavacOptionsResult> {
  val javacOptionsParams = JavacOptionsParams(allTargetsIds)
  return server.buildTargetJavacOptions(javacOptionsParams)
}


private fun <T> CompletableFuture<T>.catchSyncErrors(errorCallback: (Throwable) -> Unit): CompletableFuture<T> =
  this.whenComplete { _, exception ->
    exception?.let { errorCallback(it) }
  }