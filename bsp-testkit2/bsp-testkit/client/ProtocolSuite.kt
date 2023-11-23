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
import java.time.Duration
import java.util.concurrent.ExecutionException

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
  fun errorBeforeInitialization() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20)) { session ->
      try {
        session.server.workspaceReload().await()
      } catch (e: ExecutionException) {
        val cause = e.cause
        if (cause is ResponseErrorException) {
          assertEquals(-32002, cause.responseError.code)
        }
      }
    }
  }

  @Test
  @DisplayName("Initialization succeeds")
  fun initializationSucceeds() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20)) { session ->
      val initializationResult = session.server.buildInitialize(initializeParamsNoCapabilities).await()
      session.server.onBuildInitialized()
      session.close()
      println(initializationResult)
    }
  }

  @Test
  @DisplayName("Server exits with 0 after a shutdown request")
  fun exitAfterShutdown() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20), true) { session ->
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
  fun exitNoShutdown() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20), true) { session ->
      session.server.onBuildExit()
      val sessionResult = session.serverClosed.await()
      assertEquals(1, sessionResult.exitCode)
    }
  }

  @Test
  @DisplayName("No build targets are returned if the client has no capabilities")
  fun buildTargets() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20)) { session ->
      withLifetime(initializeParamsNoCapabilities, session) {
        val result = session.server.workspaceBuildTargets().await()
        assertTrue(result.targets.isEmpty())
      }
    }
  }

  @Test
  @DisplayName("Reload request works if is supported")
  fun reload() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20)) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        if (capabilities.canReload) {
          session.server.workspaceReload()
        }
      }
    }
  }

  @Test
  @DisplayName("Target sources list is empty if given no targets")
  fun sources() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20)) { session ->
      withLifetime(initializeParamsFullCapabilities, session) {
        val result = session.server.buildTargetSources(SourcesParams(ArrayList())).await()
        assertTrue(result.items.isEmpty())
      }
    }
  }

  @Test
  @DisplayName("Dependency sources list is empty if given no targets (if supported)")
  fun dependencySources() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20)) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        if (capabilities.dependencySourcesProvider) {
          val result = session.server.buildTargetDependencySources(DependencySourcesParams(ArrayList())).await()
          assertTrue(result.items.isEmpty())
        }
      }
    }
  }

  @Test
  @DisplayName("Dependency modules list is empty if given no targets (if supported)")
  fun dependencyModules() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20)) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        if (capabilities.dependencyModulesProvider) {
          val result = session.server.buildTargetDependencyModules(DependencyModulesParams(ArrayList())).await()
          assertTrue(result.items.isEmpty())
        }
      }
    }
  }

  @Test
  @DisplayName("Resources list is empty if given no targets (if supported)")
  fun resources() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20)) { session ->
      withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
        if (capabilities.resourcesProvider) {
          val result = session.server.buildTargetResources(ResourcesParams(ArrayList())).await()
          assertTrue(result.items.isEmpty())
        }
      }
    }
  }

  @Test
  @DisplayName("Clean cache method works")
  fun cleanCache() = runTest {
    withSession(workspacePath, Duration.ofSeconds(20)) { session ->
      withLifetime(initializeParamsFullCapabilities, session) {
        session.server.buildTargetCleanCache(CleanCacheParams(ArrayList())).await()
      }
    }
  }
}
