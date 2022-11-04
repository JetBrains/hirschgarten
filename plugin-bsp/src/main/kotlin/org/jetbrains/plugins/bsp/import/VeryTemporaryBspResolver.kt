package org.jetbrains.plugins.bsp.import

import ch.epfl.scala.bsp4j.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.connection.BspConnectionService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.BspTargetRunConsole
import org.jetbrains.plugins.bsp.ui.console.BspTargetTestConsole
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import java.util.*
import java.util.concurrent.CompletableFuture

private const val importSubtaskId = "import-subtask-id"

public class VeryTemporaryBspResolver(
  project: Project
) {

  private val projectBaseDir = project.getProjectDirOrThrow()

  // TODO
  private val server = BspConnectionService.getConnectionOrThrow(project).server!!
  private val bspConsoleService = BspConsoleService.getInstance(project)
  private val bspSyncConsole = bspConsoleService.bspSyncConsole
  private val bspBuildConsole = bspConsoleService.bspBuildConsole

  public fun runTarget(targetId: BuildTargetIdentifier): RunResult {

    val uuid = "run-" + UUID.randomUUID().toString()

    val runParams = RunParams(targetId).apply {
      originId = uuid
      arguments = listOf()
    }
    return server.buildTargetRun(runParams).get()
  }

  public fun buildTargets(targetIds: List<BuildTargetIdentifier>): CompileResult {

    val uuid = "build-" + UUID.randomUUID().toString()
    val startBuildMessage: String =
      if (targetIds.size == 1) "Building ${targetIds.first().uri}"
//      else if (targetIds.isEmpty()) "?"  // consider implementing
      else "Building ${targetIds.size} target(s)"
    bspBuildConsole.startTask(uuid, "Build", startBuildMessage)

    println("buildTargetCompile")
    val compileParams = CompileParams(targetIds).apply { originId = uuid }
    val compileResult = server.buildTargetCompile(compileParams).catchBuildErrors(uuid).get()

    when (compileResult.statusCode) {
      StatusCode.OK -> bspBuildConsole.finishTask(taskId = uuid, "Successfully completed!")
      StatusCode.CANCELLED -> bspBuildConsole.finishTask(taskId = uuid, "Cancelled!")
      StatusCode.ERROR -> bspBuildConsole.finishTask(uuid, "Ended with an error!", FailureResultImpl())
      else -> bspBuildConsole.finishTask(taskId = uuid, "Finished!")
    }

    return compileResult
  }

  public fun testTarget(targetId: BuildTargetIdentifier): TestResult {
    val params = TestParams(listOf(targetId))
    params.arguments = emptyList()
    params.originId = "test-" + UUID.randomUUID().toString()
    return server.buildTargetTest(params).get()
  }

  public fun buildTarget(targetId: BuildTargetIdentifier): CompileResult {
    return buildTargets(listOf(targetId))
  }

  public fun collectModel(taskId: Any): ProjectDetails {
    bspSyncConsole.startSubtask(taskId, importSubtaskId, "Collecting model...")
    println("buildInitialize")
    val initializeBuildResult =
      server.buildInitialize(createInitializeBuildParams()).catchSyncErrors(importSubtaskId).get()

    println("onBuildInitialized")
    server.onBuildInitialized()
    val projectDetails = collectModelWithCapabilities(initializeBuildResult.capabilities, importSubtaskId)

    bspSyncConsole.finishSubtask(importSubtaskId, "Collection model done!")
    bspSyncConsole.finishTask(taskId, "Import done!", SuccessResultImpl())

    println("done done!")
    return projectDetails
  }

  private fun collectModelWithCapabilities(
    buildServerCapabilities: BuildServerCapabilities,
    syncId: String
  ): ProjectDetails {
    println("workspaceBuildTargets")
    val workspaceBuildTargetsResult = server.workspaceBuildTargets().catchSyncErrors(syncId).get()
    val allTargetsIds = workspaceBuildTargetsResult!!.targets.map(BuildTarget::getId)

    println("buildTargetSources")
    val sourcesResult = server.buildTargetSources(SourcesParams(allTargetsIds)).catchSyncErrors(syncId).get()

    println("buildTargetResources")
    val resourcesResult =
      if (buildServerCapabilities.resourcesProvider) server.buildTargetResources(ResourcesParams(allTargetsIds))
        .catchSyncErrors(syncId).get() else null

    println("buildTargetDependencySources")
    val dependencySourcesResult =
      if (buildServerCapabilities.dependencySourcesProvider) server.buildTargetDependencySources(
        DependencySourcesParams(allTargetsIds)
      ).catchSyncErrors(syncId).get() else null

    println("buildTargetJavacOptions")
    val buildTargetJavacOptionsResult =
      server.buildTargetJavacOptions(JavacOptionsParams(allTargetsIds)).catchSyncErrors(syncId).get()
    println("done done!")
    return ProjectDetails(
      targetsId = allTargetsIds,
      targets = workspaceBuildTargetsResult.targets.toSet(),
      sources = sourcesResult.items,
      resources = resourcesResult?.items ?: emptyList(),
      dependenciesSources = dependencySourcesResult?.items ?: emptyList(),
      javacOptions = buildTargetJavacOptionsResult.items
    )
  }

  private fun createInitializeBuildParams(): InitializeBuildParams {
    val params = InitializeBuildParams(
      "IntelliJ-BSP",
      "1.0.0",
      "2.0.0",
      projectBaseDir.toString(),
      BuildClientCapabilities(listOf("java"))
    )
    val dataJson = JsonObject()
    dataJson.addProperty("clientClassesRootDir", "$projectBaseDir/out")
    dataJson.add("supportedScalaVersions", JsonArray())
    params.data = dataJson

    return params
  }

  private fun <T> CompletableFuture<T>.catchSyncErrors(syncId: String): CompletableFuture<T> {
    return this
      .whenComplete { _, exception ->
        exception?.let {
          bspSyncConsole.addMessage(importSubtaskId, "Sync failed")
          bspSyncConsole.finishTask(syncId, "Failed", FailureResultImpl(exception))
        }
      }
  }

  private fun <T> CompletableFuture<T>.catchBuildErrors(buildId: String): CompletableFuture<T> {
    return this
      .whenComplete { _, exception ->
        exception?.let {
          bspBuildConsole.addMessage("bsp-build", "Build failed")
          bspBuildConsole.finishTask(buildId, "Failed", FailureResultImpl(exception))
        }
      }
  }
}

public class BspClient(
  private val bspSyncConsole: TaskConsole,
  private val bspBuildConsole: TaskConsole,
  private val bspRunConsole: BspTargetRunConsole,
  private val bspTestConsole: BspTargetTestConsole,
) : BuildClient {

  override fun onBuildShowMessage(params: ShowMessageParams) {
    println("onBuildShowMessage")
    println(params)
    addMessageToConsole(params.originId, params.message)
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    println("onBuildLogMessage")
    println(params)
    addMessageToConsole(params.originId, params.message)
  }

  override fun onBuildTaskStart(params: TaskStartParams?) {
    when (params?.dataKind) {
      TaskDataKind.TEST_START -> {
        val gson = Gson()
        val testStart = gson.fromJson(params.data as JsonObject, TestStart::class.java)
        val isSuite = params.message.take(3) == "<S>"
        println("TEST START: ${testStart?.displayName}")
        bspTestConsole.startTest(isSuite, testStart.displayName)
      }

      TaskDataKind.TEST_TASK -> {
        // ignore
      }
    }
    println("onBuildTaskStart")
    println(params)
  }

  override fun onBuildTaskProgress(params: TaskProgressParams?) {
    println("onBuildTaskProgress")
    println(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams?) {
    when (params?.dataKind) {
      TaskDataKind.TEST_FINISH -> {
        val gson = Gson()
        val testFinish = gson.fromJson(params.data as JsonObject, TestFinish::class.java)
        val isSuite = params.message.take(3) == "<S>"
        println("TEST FINISH: ${testFinish?.displayName}")
        when (testFinish.status) {
          TestStatus.FAILED -> bspTestConsole.failTest(testFinish.displayName, testFinish.message)
          TestStatus.PASSED -> bspTestConsole.passTest(isSuite, testFinish.displayName)
          else -> bspTestConsole.ignoreTest(testFinish.displayName)
        }
      }

      TaskDataKind.TEST_REPORT -> {}
    }
    println("onBuildTaskFinish")
    println(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    println("onBuildPublishDiagnostics")
    println(params)
    addDiagnosticToConsole(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget?) {
    println("onBuildTargetDidChange")
    println(params)
  }

  private fun addMessageToConsole(originId: String?, message: String) {
    if (originId?.startsWith("build") == true) {
      bspBuildConsole.addMessage(originId, message)
    } else if (originId?.startsWith("test") == true) {
      bspTestConsole.print(message)
    } else if (originId?.startsWith("run") == true) {
      bspRunConsole.print(message)
    } else {
      bspSyncConsole.addMessage(originId ?: importSubtaskId, message)
    }
  }

  private fun addDiagnosticToConsole(params: PublishDiagnosticsParams) {
    if (params.originId != null && params.textDocument != null) {
      val targetConsole = if (params.originId?.startsWith("build") == true) bspBuildConsole else bspSyncConsole
      params.diagnostics.forEach {
        targetConsole.addDiagnosticMessage(
          params.originId,
          params.textDocument.uri,
          it.range.start.line,
          it.range.start.character,
          it.message,
          getMessageEventKind(it.severity)
        )
      }
    }
  }

  private fun getMessageEventKind(severity: DiagnosticSeverity?): MessageEvent.Kind =
    when (severity) {
      DiagnosticSeverity.ERROR -> MessageEvent.Kind.ERROR
      DiagnosticSeverity.WARNING -> MessageEvent.Kind.WARNING
      DiagnosticSeverity.INFORMATION -> MessageEvent.Kind.INFO
      DiagnosticSeverity.HINT -> MessageEvent.Kind.INFO
      null -> MessageEvent.Kind.SIMPLE
    }
}