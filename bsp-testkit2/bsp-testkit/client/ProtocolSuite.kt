package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.ExecutionException
import kotlin.time.Duration.Companion.seconds

/**
 * This file contains test which are not specific to any language or build tool.
 */
class ProtocolSuite(private val workspacePath: Path) {
  private val executionContext = java.util.concurrent.Executors.newCachedThreadPool()
  private val bspVersion = "2.0.0"
  private val originId = "TestOriginId"

  private val initializeParamsNoCapabilities = InitializeBuildParams(
    "TestClient",
    "1.0.0",
    bspVersion,
    workspacePath.toString(),
    BuildClientCapabilities(listOf())
  )

  private val initializeParamsFullCapabilities = InitializeBuildParams(
    "TestClient",
    "1.0.0",
    bspVersion,
    workspacePath.toString(),
    BuildClientCapabilities(listOf("java", "scala", "kotlin", "cpp", "python", "thrift", "rust"))
  )

  private enum class ExecutionTestType {
    ORIGIN_ID_MATCH,
    TASK_MATCH,
    DIAGNOSTICS,
    PROGRESS,
    PROGRESS_TOTAL,
  }

  private suspend fun testCompileRequest(
    session: Session,
    capabilities: BuildServerCapabilities,
    testType: ExecutionTestType,
  ) {
    val targets = session.server.workspaceBuildTargets().await().targets
    val compileCapabilities = capabilities.compileProvider.languageIds
    val buildTargetToCompile = targets.firstOrNull {
      t -> t.languageIds.all { it in compileCapabilities } && t.capabilities.canCompile
    }
    if (buildTargetToCompile != null) {
      val compileParams = CompileParams(listOf(buildTargetToCompile.id))
      compileParams.originId = originId
      val result = session.server.buildTargetCompile(compileParams).await()

      checkExecutionAssertions(session, result.originId, testType)
    }
  }

  private suspend fun testRunRequest(
    session: Session,
    capabilities: BuildServerCapabilities,
    testType: ExecutionTestType,
  ) {
    val targets = session.server.workspaceBuildTargets().await().targets
    val runCapabilities = capabilities.runProvider.languageIds
    val buildTargetToRun = targets.firstOrNull {
      t -> t.languageIds.all { it in runCapabilities } && t.capabilities.canRun
    }
    if (buildTargetToRun != null) {
      val runParams = RunParams(buildTargetToRun.id)
      runParams.originId = originId
      val result = session.server.buildTargetRun(runParams).await()

      checkExecutionAssertions(session, result.originId, testType)
    }
  }

  private suspend fun testTestRequest(
    session: Session,
    capabilities: BuildServerCapabilities,
    testType: ExecutionTestType,
  ) {
    val targets = session.server.workspaceBuildTargets().await().targets
    val testCapabilities = capabilities.testProvider.languageIds
    val buildTargetToTest = targets.firstOrNull {
      t -> t.languageIds.all { it in testCapabilities } && t.capabilities.canTest
    }
    if (buildTargetToTest != null) {
      val testParams = TestParams(listOf(buildTargetToTest.id))
      testParams.originId = originId
      val result = session.server.buildTargetTest(testParams).await()

      checkExecutionAssertions(session, result.originId, testType)
    }
  }

  private fun checkExecutionAssertions(
    session: Session,
    resultOriginId: String,
    testType: ExecutionTestType,
  ) {
    val started = session.client.taskStartNotifications.map { it.taskId }
    val finished = session.client.taskFinishNotifications.map { it.taskId }
    val progress = session.client.taskProgressNotifications
    val diagnosticsOriginIds = session.client.publishDiagnosticsNotifications.map { it.originId }

    when(testType) {
      ExecutionTestType.ORIGIN_ID_MATCH -> assertEquals(originId, resultOriginId)
      ExecutionTestType.TASK_MATCH -> assertTrue(started.size == finished.size && started.containsAll(finished) && finished.containsAll(started))
      ExecutionTestType.DIAGNOSTICS -> assertTrue(diagnosticsOriginIds.all { it == originId })
      ExecutionTestType.PROGRESS -> assertTrue(progress.map { it.taskId }.all { started.contains(it) })
      ExecutionTestType.PROGRESS_TOTAL -> assertTrue(progress.all { it.progress <= it.total })
    }
  }

  @Test
  @DisplayName("Before initialization server responds with an error")
  fun errorBeforeInitialization() = runTest(timeout = 20.seconds) {
    println("Before initialization server responds with an error")
    withSession(workspacePath, true, false) { session ->
      try {
        session.server.workspaceReload().await()
      } catch (e: ResponseErrorException) {
        assertEquals(-32002, e.responseError.code)
        println("Properly caught error")
      } finally {
        session.close()
      }
    }
  }

  @Test
  @DisplayName("Initialization succeeds")
  fun initializationSucceeds() = runTest(timeout = 20.seconds) {
    println("Initialization succeeds")
    withSession(workspacePath) { session ->
      val initializationResult = session.server.buildInitialize(initializeParamsNoCapabilities).await()
      session.server.onBuildInitialized()
      session.server.buildShutdown().await()
      session.server.onBuildExit()
      println(initializationResult)
    }
  }

  @Test
  @DisplayName("Server exits with 0 after a shutdown request")
  fun exitAfterShutdown() = runTest(timeout = 20.seconds) {
    println("Server exits with 0 after a shutdown request")
    withSession(workspacePath, true) { session ->
      session.server.buildInitialize(initializeParamsNoCapabilities).await()
      session.server.onBuildInitialized()
      session.server.buildShutdown().await()
      session.server.onBuildExit()
      val sessionResult = session.serverClosed.await()
      assertEquals(0, sessionResult.exitCode)
    }
  }

  @Test
  @DisplayName("Server exits with 1 without a shutdown request")
  fun exitNoShutdown() = runTest(timeout = 20.seconds) {
    println("Server exits with 1 without a shutdown request")
    withSession(workspacePath, true) { session ->
      session.server.buildInitialize(initializeParamsNoCapabilities).await()
      session.server.onBuildInitialized()
      session.server.onBuildExit()
      val sessionResult = session.serverClosed.await()
      assertEquals(1, sessionResult.exitCode)
    }
  }

  @Test
  @DisplayName("Server exits with 0 without initialization")
  fun exitNoInitialization() = runTest(timeout = 20.seconds) {
    println("Server exits with 0 without initialization")
    withSession(workspacePath, true) { session ->
      session.server.onBuildExit()
      val sessionResult = session.serverClosed.await()
      assertEquals(0, sessionResult.exitCode)
    }
  }

  @Test
  @DisplayName("No build targets are returned if the client has no capabilities")
  fun buildTargets() = runTest(timeout = 20.seconds) {
    println("No build targets are returned if the client has no capabilities")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsNoCapabilities, session) {
        val result = session.server.workspaceBuildTargets().await()
        assertTrue(result.targets.all { it.languageIds.isEmpty() })
      }
    }
  }

  @Test
  @DisplayName("Reload request works if is supported")
  fun reload() = runTest(timeout = 20.seconds) {
    println("Reload request works if is supported")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        if (capabilities.canReload == true) {
          session.server.workspaceReload()
        }
      }
    }
  }

  @Test
  @DisplayName("Target sources list is empty if given no targets")
  fun sources() = runTest(timeout = 20.seconds) {
    println("Target sources list is empty if given no targets")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) {
        val result = session.server.buildTargetSources(SourcesParams(ArrayList())).await()
        assertTrue(result.items.isEmpty())
      }
    }
  }

  @Test
  @DisplayName("Dependency sources list is empty if given no targets (if supported)")
  fun dependencySources() = runTest(timeout = 20.seconds) {
    println("Dependency sources list is empty if given no targets (if supported)")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        if (capabilities.dependencySourcesProvider == true) {
          val result = session.server.buildTargetDependencySources(DependencySourcesParams(ArrayList())).await()
          assertTrue(result.items.isEmpty())
        }
      }
    }
  }

  @Test
  @DisplayName("Dependency modules list is empty if given no targets (if supported)")
  fun dependencyModules() = runTest(timeout = 20.seconds) {
    println("Dependency modules list is empty if given no targets (if supported)")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        if (capabilities.dependencyModulesProvider == true) {
          val result = session.server.buildTargetDependencyModules(DependencyModulesParams(ArrayList())).await()
          assertTrue(result.items.isEmpty())
        }
      }
    }
  }

  @Test
  @DisplayName("Resources list is empty if given no targets (if supported)")
  fun resources() = runTest(timeout = 20.seconds) {
    println("Resources list is empty if given no targets (if supported)")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        if (capabilities.resourcesProvider == true) {
          val result = session.server.buildTargetResources(ResourcesParams(ArrayList())).await()
          assertTrue(result.items.isEmpty())
        }
      }
    }
  }

  @Test
  @DisplayName("Output paths list is empty if given no targets (if supported)")
  fun outputPaths() = runTest(timeout = 20.seconds) {
    println("Output paths list is empty if given no targets (if supported)")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        if (capabilities.outputPathsProvider == true) {
          val result = session.server.buildTargetOutputPaths(OutputPathsParams(ArrayList())).await()
          assertTrue(result.items.isEmpty())
        }
      }
    }
  }

  @Test
  @DisplayName("Clean cache method works")
  fun cleanCache() = runTest(timeout = 20.seconds) {
    println("Clean cache method works")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) {
        session.server.buildTargetCleanCache(CleanCacheParams(ArrayList())).await()
      }
    }
  }

  @Test
  @DisplayName("OriginId should match in CompileParams and CompileResult")
  fun compileMatchingOriginId() = runTest(timeout = 40.seconds) {
    println("OriginId should match in CompileParams and CompileResult")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testCompileRequest(session, capabilities, ExecutionTestType.ORIGIN_ID_MATCH)
      }
    }
  }

  @Test
  @DisplayName("OriginId should match in RunParams and RunResult")
  fun runMatchingOriginId() = runTest(timeout = 40.seconds) {
    println("OriginId should match in RunParams and RunResult")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testRunRequest(session, capabilities, ExecutionTestType.ORIGIN_ID_MATCH)
      }
    }
  }

  @Test
  @DisplayName("OriginId should match in TestParams and TesteResult")
  fun testMatchingOriginId() = runTest(timeout = 40.seconds) {
    println("OriginId should match in TestParams and TestResult")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testTestRequest(session, capabilities, ExecutionTestType.ORIGIN_ID_MATCH)
      }
    }
  }

  @Test
  @DisplayName("For each TaskStart there should be TaskFinish for compile request")
  fun compileTaskMatching() = runTest(timeout = 40.seconds) {
    println("For each TaskStart there should be TaskFinish for compile request")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testCompileRequest(session, capabilities, ExecutionTestType.TASK_MATCH)
      }
    }
  }

  @Test
  @DisplayName("For each TaskStart there should be TaskFinish for run request")
  fun runTaskMatching() = runTest(timeout = 40.seconds) {
    println("For each TaskStart there should be TaskFinish for run request")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testRunRequest(session, capabilities, ExecutionTestType.TASK_MATCH)
      }
    }
  }

  @Test
  @DisplayName("For each TaskStart there should be TaskFinish for test request")
  fun testTaskMatching() = runTest(timeout = 40.seconds) {
    println("For each TaskStart there should be TaskFinish for test request")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testTestRequest(session, capabilities, ExecutionTestType.TASK_MATCH)
      }
    }
  }

  @Test
  @DisplayName("OriginId should match in CompileParams and publish diagnostic notifications")
  fun diagnosticsMatchingOriginId() = runTest(timeout = 40.seconds) {
    println("OriginId should match in CompileParams and publish diagnostic notifications")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testCompileRequest(session, capabilities, ExecutionTestType.DIAGNOSTICS)
      }
    }
  }

  @Test
  @DisplayName("Task progress refers to started task for compile request")
  fun compileProgressTaskFromStarted() = runTest(timeout = 40.seconds) {
    println("Task progress refers to started task for compile request")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testCompileRequest(session, capabilities, ExecutionTestType.PROGRESS)
      }
    }
  }

  @Test
  @DisplayName("Task progress refers to started task for run request")
  fun runProgressTaskFromStarted() = runTest(timeout = 40.seconds) {
    println("Task progress refers to started task for run request")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testRunRequest(session, capabilities, ExecutionTestType.PROGRESS)
      }
    }
  }

  @Test
  @DisplayName("Task progress refers to started task for test request")
  fun testProgressTaskFromStarted() = runTest(timeout = 40.seconds) {
    println("Task progress refers to started task for test request")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testTestRequest(session, capabilities, ExecutionTestType.PROGRESS)
      }
    }
  }

  @Test
  @DisplayName("Completed amount of work from task progress is smaller than total for compile request")
  fun compileProgressSmallerThanTotal() = runTest(timeout = 40.seconds) {
    println("Completed amount of work from task progress is smaller than total for compile request")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testCompileRequest(session, capabilities, ExecutionTestType.PROGRESS_TOTAL)
      }
    }
  }

  @Test
  @DisplayName("Completed amount of work from task progress is smaller than total for run request")
  fun runProgressSmallerThanTotal() = runTest(timeout = 40.seconds) {
    println("Completed amount of work from task progress is smaller than total for run request")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testRunRequest(session, capabilities, ExecutionTestType.PROGRESS_TOTAL)
      }
    }
  }

  @Test
  @DisplayName("Completed amount of work from task progress is smaller than total for test request")
  fun testProgressSmallerThanTotal() = runTest(timeout = 40.seconds) {
    println("Completed amount of work from task progress is smaller than total for test request")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        testTestRequest(session, capabilities, ExecutionTestType.PROGRESS_TOTAL)
      }
    }
  }
}

fun main(args: Array<String>) {
  if (args.size != 1) {
    println("Invalid number of arguments. Pass the path to project's directory.")
    return
  }
  val workspacePath = Paths.get(args[0])
  val protocolSuite = ProtocolSuite(workspacePath)
  protocolSuite.errorBeforeInitialization()
  protocolSuite.initializationSucceeds()
  protocolSuite.exitAfterShutdown()
  protocolSuite.exitNoShutdown()
  protocolSuite.exitNoInitialization()
  protocolSuite.buildTargets()
  protocolSuite.reload()
  protocolSuite.sources()
  protocolSuite.dependencySources()
  protocolSuite.dependencyModules()
  protocolSuite.resources()
  protocolSuite.outputPaths()
  protocolSuite.compileMatchingOriginId()
  protocolSuite.runMatchingOriginId()
  protocolSuite.testMatchingOriginId()
  protocolSuite.compileTaskMatching()
  protocolSuite.runTaskMatching()
  protocolSuite.testTaskMatching()
  protocolSuite.diagnosticsMatchingOriginId()
  protocolSuite.compileProgressTaskFromStarted()
  protocolSuite.runProgressTaskFromStarted()
  protocolSuite.testProgressTaskFromStarted()
  protocolSuite.compileProgressSmallerThanTotal()
  protocolSuite.runProgressSmallerThanTotal()
  protocolSuite.testProgressSmallerThanTotal()
  protocolSuite.cleanCache()
}
