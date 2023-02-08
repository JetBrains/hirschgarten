package org.jetbrains.plugins.bsp.server.client

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CleanCacheParams
import ch.epfl.scala.bsp4j.CleanCacheResult
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileReport
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.CompileTask
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
import ch.epfl.scala.bsp4j.TaskDataKind
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskId
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.google.gson.Gson
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
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
  val gson = Gson()

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

  @Test
  fun `compile task correctly reports subtasks`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)
    val subtaskId = TaskId("subtask1")
    subtaskId.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(subtaskId)
    listener.onBuildTaskStart(subtaskStart)
    val nowLong = Instant.now().toEpochMilli()
    val now = Instant.ofEpochMilli(nowLong)
    val subtaskProgress = TaskProgressParams(subtaskId)
    subtaskProgress.eventTime = nowLong
    subtaskProgress.message = "subtask1 progress"
    listener.onBuildTaskProgress(subtaskProgress)
    val subtaskFinish = TaskFinishParams(subtaskId, StatusCode.OK)
    subtaskFinish.eventTime = nowLong
    listener.onBuildTaskFinish(subtaskFinish)

    val compileSubtaskId = TaskId("subtask2")
    compileSubtaskId.parents = listOf(compileTask.originId.id)
    val compileSubtaskStart = TaskStartParams(compileSubtaskId)
    compileSubtaskStart.dataKind = TaskDataKind.COMPILE_TASK
    val compileSubtaskTarget = BuildTargetIdentifier("target")
    val compileSubtaskData = CompileTask(compileSubtaskTarget)
    compileSubtaskStart.data = gson.toJsonTree(compileSubtaskData)
    listener.onBuildTaskStart(compileSubtaskStart)
    val compileSubtaskFinish = TaskFinishParams(compileSubtaskId, StatusCode.OK)
    compileSubtaskFinish.dataKind = TaskDataKind.COMPILE_REPORT
    val compileSubtaskReport = CompileReport(compileSubtaskTarget, 1, 2)
    compileSubtaskFinish.data = gson.toJsonTree(compileSubtaskReport)
    listener.onBuildTaskFinish(compileSubtaskFinish)

    val result = CompileResult(StatusCode.OK)
    server.finishCompileTask(compileTask.originId, result)

    observer.genericTaskStartedNotifications shouldBe listOf(
      ClientGenericTaskStartedParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), null, null),
    )

    observer.genericTaskProgressNotifications shouldBe listOf(
      ClientGenericTaskProgressParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), now, "subtask1 progress", null, null, null)
    )

    observer.genericTaskFinishedNotifications shouldBe listOf(
      ClientGenericTaskFinishedParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), now, null, StatusCode.OK)
    )

    observer.compileTaskStartedNotifications shouldBe listOf(
      ClientCompileTaskStartedParams(
        ClientTaskId(compileSubtaskId.id), ClientTaskId(compileTask.originId.id),
        null, null, compileSubtaskTarget
      )
    )
    observer.compileTaskFinishedNotifications shouldBe listOf(
      ClientCompileTaskFinishedParams(
        ClientTaskId(compileSubtaskId.id), ClientTaskId(compileTask.originId.id),
        null, null, StatusCode.OK, compileSubtaskTarget, 1, 2, null, null
      )
    )
  }

  @Test
  fun `compile task correctly reports nested subtasks`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)
    val subtaskId = TaskId("subtask1")
    subtaskId.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(subtaskId)
    listener.onBuildTaskStart(subtaskStart)

    val subSubtask1Id = TaskId("subsubtask1")
    subSubtask1Id.parents = listOf(subtaskId.id)
    val subSubTask1Start = TaskStartParams(subSubtask1Id)
    listener.onBuildTaskStart(subSubTask1Start)

    val subSubtask2Id = TaskId("subsubtask2")
    subSubtask2Id.parents = listOf(subtaskId.id)
    val subSubTask2Start = TaskStartParams(subSubtask2Id)
    listener.onBuildTaskStart(subSubTask2Start)

    listener.onBuildTaskFinish(TaskFinishParams(subSubtask1Id, StatusCode.OK))
    listener.onBuildTaskFinish(TaskFinishParams(subSubtask2Id, StatusCode.OK))
    listener.onBuildTaskFinish(TaskFinishParams(subtaskId, StatusCode.OK))

    val result = CompileResult(StatusCode.OK)
    server.finishCompileTask(compileTask.originId, result)

    observer.genericTaskStartedNotifications shouldBe listOf(
      ClientGenericTaskStartedParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), null, null),
      ClientGenericTaskStartedParams(ClientTaskId(subSubtask1Id.id), ClientTaskId(subtaskId.id), null, null),
      ClientGenericTaskStartedParams(ClientTaskId(subSubtask2Id.id), ClientTaskId(subtaskId.id), null, null),
    )

    observer.genericTaskFinishedNotifications shouldBe listOf(
      ClientGenericTaskFinishedParams(ClientTaskId(subSubtask1Id.id), ClientTaskId(subtaskId.id), null, null, StatusCode.OK),
      ClientGenericTaskFinishedParams(ClientTaskId(subSubtask2Id.id), ClientTaskId(subtaskId.id), null, null, StatusCode.OK),
      ClientGenericTaskFinishedParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), null, null, StatusCode.OK),
    )
  }

  @Test
  fun `compile task correctly reports nestead subtasks`() {

  }
}
