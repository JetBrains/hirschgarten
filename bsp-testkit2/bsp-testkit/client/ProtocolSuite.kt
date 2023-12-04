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
    BuildClientCapabilities(listOf("java", "scala", "kotlin"))
  )

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
  @DisplayName("Clean cache method works")
  fun cleanCache() = runTest(timeout = 20.seconds) {
    println("Clean cache method works")
    withSession(workspacePath) { session ->
      withLifetime(initializeParamsFullCapabilities, session) {
        session.server.buildTargetCleanCache(CleanCacheParams(ArrayList())).await()
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
  protocolSuite.buildTargets()
  protocolSuite.reload()
  protocolSuite.sources()
  protocolSuite.dependencySources()
  protocolSuite.dependencyModules()
  protocolSuite.resources()
  protocolSuite.cleanCache()
}
