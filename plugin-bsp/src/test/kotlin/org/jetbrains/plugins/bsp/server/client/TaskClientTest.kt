package org.jetbrains.plugins.bsp.server.client

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.CleanCacheParams
import ch.epfl.scala.bsp4j.CleanCacheResult
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.DebugSessionAddress
import ch.epfl.scala.bsp4j.DebugSessionParams
import ch.epfl.scala.bsp4j.DependencyModulesParams
import ch.epfl.scala.bsp4j.DependencyModulesResult
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.InitializeBuildResult
import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

private class MockServer : BuildServer {

  val compileTasks = mutableMapOf<OriginId, CompletableFuture<CompileResult>>()

  override fun buildTargetCompile(params: CompileParams): CompletableFuture<CompileResult> {
    val originId = OriginId(params.originId)
    val compileTask = CompletableFuture<CompileResult>()
    compileTasks[originId] = compileTask
    return compileTask
  }

  fun finishCompileTask(originId: OriginId, result: CompileResult) {
    compileTasks[originId]?.complete(result)
  }

  fun cancelCompileTask(originId: OriginId) {
    compileTasks[originId]?.cancel(true)
  }

  fun abortCompileTask(originId: OriginId, throwable: Throwable) {
    compileTasks[originId]?.completeExceptionally(throwable)
  }

  override fun buildTargetTest(params: TestParams?): CompletableFuture<TestResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetRun(params: RunParams?): CompletableFuture<RunResult> {
    TODO("Not yet implemented")
  }

  // Not needed for tasks

  override fun buildInitialize(params: InitializeBuildParams?): CompletableFuture<InitializeBuildResult> {
    TODO("Not yet implemented")
  }

  override fun onBuildInitialized() {
    TODO("Not yet implemented")
  }

  override fun buildShutdown(): CompletableFuture<Any> {
    TODO("Not yet implemented")
  }

  override fun onBuildExit() {
    TODO("Not yet implemented")
  }

  override fun workspaceBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> {
    TODO("Not yet implemented")
  }

  override fun workspaceReload(): CompletableFuture<Any> {
    TODO("Not yet implemented")
  }

  override fun buildTargetSources(params: SourcesParams?): CompletableFuture<SourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetInverseSources(params: InverseSourcesParams?): CompletableFuture<InverseSourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetDependencySources(params: DependencySourcesParams?): CompletableFuture<DependencySourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetResources(params: ResourcesParams?): CompletableFuture<ResourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetOutputPaths(params: OutputPathsParams?): CompletableFuture<OutputPathsResult> {
    TODO("Not yet implemented")
  }

  override fun debugSessionStart(params: DebugSessionParams?): CompletableFuture<DebugSessionAddress> {
    TODO("Not yet implemented")
  }

  override fun buildTargetCleanCache(params: CleanCacheParams?): CompletableFuture<CleanCacheResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetDependencyModules(params: DependencyModulesParams?): CompletableFuture<DependencyModulesResult> {
    TODO("Not yet implemented")
  }
}

class MockCompileTaskObserver : CompileTaskObserver {
  val compileTaskStartedNotifications = mutableListOf<ClientCompileTaskStartedParams>()
  val compileTaskFinishedNotifications = mutableListOf<ClientCompileTaskFinishedParams>()
  val topLevelCompileTaskFinishedNotifications = mutableListOf<ClientTopLevelCompileTaskFinishedParams>()
  val genericTaskStartedNotifications = mutableListOf<ClientGenericTaskStartedParams>()
  val genericTaskProgressNotifications = mutableListOf<ClientGenericTaskProgressParams>()
  val genericTaskFinishedNotifications = mutableListOf<ClientGenericTaskFinishedParams>()
  val topLevelTaskFailedNotifications = mutableListOf<Throwable>()

  override fun onCompileTaskStarted(params: ClientCompileTaskStartedParams) {
    compileTaskStartedNotifications.add(params)
  }

  override fun onCompileTaskFinished(params: ClientCompileTaskFinishedParams) {
    compileTaskFinishedNotifications.add(params)
  }

  override fun onTopLevelCompileTaskFinished(params: ClientTopLevelCompileTaskFinishedParams) {
    topLevelCompileTaskFinishedNotifications.add(params)
  }

  override fun onTaskStarted(params: ClientGenericTaskStartedParams) {
    genericTaskStartedNotifications.add(params)
  }

  override fun onTaskProgress(params: ClientGenericTaskProgressParams) {
    genericTaskProgressNotifications.add(params)
  }

  override fun onTaskFinished(params: ClientGenericTaskFinishedParams) {
    genericTaskFinishedNotifications.add(params)
  }

  override fun onTopLevelTaskFailed(throwable: Throwable) {
    topLevelTaskFailedNotifications.add(throwable)
  }
}

class TaskClientTest {
  private val server = MockServer()
  private val client = TaskClient(server)
  private val listener = client.BspClientListener()

  @Test
  fun `compile task correctly starts and finishes`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)
    val result = CompileResult(StatusCode.OK)
    server.finishCompileTask(compileTask.originId, result)
    observer.compileTaskStartedNotifications shouldBe emptyList()
    observer.compileTaskFinishedNotifications shouldBe emptyList()
    observer.topLevelCompileTaskFinishedNotifications shouldBe listOf(
      ClientTopLevelCompileTaskFinishedParams(result.statusCode)
    )
    observer.genericTaskStartedNotifications shouldBe emptyList()
    observer.genericTaskProgressNotifications shouldBe emptyList()
    observer.genericTaskFinishedNotifications shouldBe emptyList()
    observer.topLevelTaskFailedNotifications shouldBe emptyList()
  }
}
