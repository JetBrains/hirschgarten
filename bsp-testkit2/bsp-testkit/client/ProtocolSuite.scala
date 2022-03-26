package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j._
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.jetbrains.bsp.testkit.client.TestClient.{withLifetime, withSession}
import org.junit.jupiter.api.Assertions.{assertEquals, assertTrue}
import org.junit.jupiter.api.{DisplayName, Test}

import java.nio.file.Path
import java.time.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters.CompletionStageOps

/** This file contains test which are not specific to any language or build tool.
 */
class ProtocolSuite(workspacePath: Path) {
  private implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

  val bspVersion = "2.0.0"

  val initializeParamsNoCapabilities = new InitializeBuildParams(
    "TestClient",
    "1.0.0",
    bspVersion,
    workspacePath.toString,
    new BuildClientCapabilities(List.empty.asJava)
  )
  val initializeParamsFullCapabilities = new InitializeBuildParams(
    "TestClient",
    "1.0.0",
    bspVersion,
    workspacePath.toString,
    new BuildClientCapabilities(List("java", "scala", "kotlin").asJava)
  )

  @Test
  @DisplayName("Before initialization server responds with an error")
  def errorBeforeInitialization(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20)) { session =>
      session.server.workspaceReload().asScala.failed.map { case e: ResponseErrorException =>
        assertEquals(-32002, e.getResponseError.getCode)
      }
    }
  }

  @Test
  @DisplayName("Initialization succeeds")
  def initializationSucceeds(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20)) { session =>
      for {
        initializationResult <- session.server
          .buildInitialize(initializeParamsNoCapabilities)
          .asScala
        _ = session.server.onBuildInitialized()
        _ = session.close()
      } yield {
        println(initializationResult)
      }
    }
  }

  @Test
  @DisplayName("Server exits with 0 after a shutdown request")
  def exitAfterShutdown(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20), ignoreEarlyExit = true) { session =>
      for {
        _ <- session.server.buildInitialize(initializeParamsNoCapabilities).asScala
        _ = session.server.onBuildInitialized()
        _ <- session.server.buildShutdown().asScala
        _ = session.server.onBuildExit()
        SessionResult(exitCode, _) <- session.serverClosed
      } yield {
        assertEquals(0, exitCode)
      }
    }
  }

  @Test
  @DisplayName("Server exits with 1 without a shutdown request")
  def exitNoShutdown(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20), ignoreEarlyExit = true) { session =>
      session.server.onBuildExit()
      for {
        SessionResult(exitCode, _) <- session.serverClosed
      } yield {
        assertEquals(1, exitCode)
      }
    }
  }

  @Test
  @DisplayName("No build targets are returned if the client has no capabilities")
  def buildTargets(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20)) { session =>
      withLifetime(initializeParamsNoCapabilities, session) { _ =>
        for {
          result <- session.server.workspaceBuildTargets().asScala
        } yield {
          assertTrue(result.getTargets.isEmpty)
        }
      }
    }
  }

  @Test
  @DisplayName("Reload request works if is supported")
  def reload(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20)) { session =>
      withLifetime(initializeParamsFullCapabilities, session) { capabilities =>
        if (capabilities.getCanReload) {
          for {
            _ <- session.server.workspaceReload().asScala
          } yield ()
        } else {
          Future.successful(())
        }
      }
    }
  }

  @Test
  @DisplayName("Target sources list is empty if given no targets")
  def sources(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20)) { session =>
      withLifetime(initializeParamsFullCapabilities, session) { _ =>
        for {
          result <- session.server.buildTargetSources(new SourcesParams(List.empty.asJava)).asScala
        } yield {
          assertTrue(result.getItems.isEmpty)
        }
      }
    }
  }

  @Test
  @DisplayName("Dependency sources list is empty if given no targets (if supported)")
  def dependencySources(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20)) { session =>
      withLifetime(initializeParamsFullCapabilities, session) { capabilities =>
        if (capabilities.getDependencySourcesProvider) {
          for {
            result <- session.server
              .buildTargetDependencySources(new DependencySourcesParams(List.empty.asJava))
              .asScala
          } yield {
            assertTrue(result.getItems.isEmpty)
          }
        } else {
          Future.successful(())
        }
      }
    }
  }

  @Test
  @DisplayName("Dependency modules list is empty if given no targets (if supported)")
  def dependencyModules(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20)) { session =>
      withLifetime(initializeParamsFullCapabilities, session) { capabilities =>
        if (capabilities.getDependencyModulesProvider) {
          for {
            result <- session.server
              .buildTargetDependencyModules(new DependencyModulesParams(List.empty.asJava))
              .asScala
          } yield {
            assertTrue(result.getItems.isEmpty)
          }
        } else {
          Future.successful(())
        }
      }
    }
  }

  @Test
  @DisplayName("Resources list is empty if given no targets (if supported)")
  def resources(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20)) { session =>
      withLifetime(initializeParamsFullCapabilities, session) { capabilities =>
        if (capabilities.getResourcesProvider) {
          for {
            result <- session.server
              .buildTargetResources(new ResourcesParams(List.empty.asJava))
              .asScala
          } yield {
            assertTrue(result.getItems.isEmpty)
          }
        } else {
          Future.successful(())
        }
      }
    }
  }

  @Test
  @DisplayName("Clean cache method works")
  def cleanCache(): Unit = {
    withSession(workspacePath, Duration.ofSeconds(20)) { session =>
      withLifetime(initializeParamsFullCapabilities, session) { _ =>
        for {
          _ <- session.server.buildTargetCleanCache(new CleanCacheParams(List.empty.asJava)).asScala
        } yield ()
      }
    }
  }
}

