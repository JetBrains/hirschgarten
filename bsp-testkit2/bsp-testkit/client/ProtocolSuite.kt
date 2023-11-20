package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.*
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.jetbrains.bsp.testkit.client.TestClient.withLifetime
import org.jetbrains.bsp.testkit.client.TestClient.withSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.collections.ArrayList

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
        BuildClientCapabilities(ArrayList())
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
    fun errorBeforeInitialization() {
        withSession(workspacePath, Duration.ofSeconds(20)) { session ->
            try {
                session.server.workspaceReload().get()
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
    fun initializationSucceeds() {
        withSession(workspacePath, Duration.ofSeconds(20)) { session ->
            val initializationResult = session.server.buildInitialize(initializeParamsNoCapabilities).get()
            session.server.onBuildInitialized()
            session.close()
            println(initializationResult)
        }
    }

    @Test
    @DisplayName("Server exits with 0 after a shutdown request")
    fun exitAfterShutdown() {
        withSession(workspacePath, Duration.ofSeconds(20), true) { session ->
            session.server.buildInitialize(initializeParamsNoCapabilities).get()
            session.server.onBuildInitialized()
            session.server.buildShutdown().get()
            session.server.onBuildExit()
            val sessionResult = session.serverClosed.get()
            assertEquals(0, sessionResult.exitCode)
        }
    }

    @Test
    @DisplayName("Server exits with 1 without a shutdown request")
    fun exitNoShutdown() {
        withSession(workspacePath, Duration.ofSeconds(20), true) { session ->
            session.server.onBuildExit()
            val sessionResult = session.serverClosed.get()
            assertEquals(1, sessionResult.exitCode)
        }
    }

    @Test
    @DisplayName("No build targets are returned if the client has no capabilities")
    fun buildTargets() {
        withSession(workspacePath, Duration.ofSeconds(20)) { session ->
            withLifetime(initializeParamsNoCapabilities, session) {
                val result = session.server.workspaceBuildTargets().get()
                assertTrue(result.targets.isEmpty())
            }
        }
    }

    @Test
    @DisplayName("Reload request works if is supported")
    fun reload() {
        withSession(workspacePath, Duration.ofSeconds(20)) { session ->
            withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
                if (capabilities.canReload) {
                    session.server.workspaceReload().get()
                }
            }
        }
    }

    @Test
    @DisplayName("Target sources list is empty if given no targets")
    fun sources() {
        withSession(workspacePath, Duration.ofSeconds(20)) { session ->
            withLifetime(initializeParamsFullCapabilities, session) {
                val result = session.server.buildTargetSources(SourcesParams(ArrayList())).get()
                assertTrue(result.items.isEmpty())
            }
        }
    }

    @Test
    @DisplayName("Dependency sources list is empty if given no targets (if supported)")
    fun dependencySources() {
        withSession(workspacePath, Duration.ofSeconds(20)) { session ->
            withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
                if (capabilities.dependencySourcesProvider) {
                    val result = session.server.buildTargetDependencySources(DependencySourcesParams(ArrayList())).get()
                    assertTrue(result.items.isEmpty())
                }
            }
        }
    }

    @Test
    @DisplayName("Dependency modules list is empty if given no targets (if supported)")
    fun dependencyModules() {
        withSession(workspacePath, Duration.ofSeconds(20)) { session ->
            withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
                if (capabilities.dependencyModulesProvider) {
                    val result = session.server.buildTargetDependencyModules(DependencyModulesParams(ArrayList())).get()
                    assertTrue(result.items.isEmpty())
                }
            }
        }
    }

    @Test
    @DisplayName("Resources list is empty if given no targets (if supported)")
    fun resources() {
        withSession(workspacePath, Duration.ofSeconds(20)) { session ->
            withLifetime(initializeParamsFullCapabilities, session) { capabilities ->
                if (capabilities.resourcesProvider) {
                    val result = session.server.buildTargetResources(ResourcesParams(ArrayList())).get()
                    assertTrue(result.items.isEmpty())
                }
            }
        }
    }

    @Test
    @DisplayName("Clean cache method works")
    fun cleanCache() {
        withSession(workspacePath, Duration.ofSeconds(20)) { session ->
            withLifetime(initializeParamsFullCapabilities, session) {
                session.server.buildTargetCleanCache(CleanCacheParams(ArrayList())).get()
            }
        }
    }
}