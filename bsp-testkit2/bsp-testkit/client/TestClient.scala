package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j._
import com.google.gson.GsonBuilder
import org.jetbrains.bsp.testkit.client.TestClient.{withLifetime, withSession}
import org.junit.jupiter.api.Assertions.assertEquals

import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ExecutionException
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}
import scala.jdk.DurationConverters._
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.{Failure, Success}

object TestClient {
  def withSession(workspace: Path, timeout: Duration, ignoreEarlyExit: Boolean = false)(
    test: Session => Future[Unit]
  )(implicit ec: ExecutionContext): Unit = {
    val session = new Session(workspace)
    val testResult = test(session)
    val serverClosed = session.serverClosed.transform {
      case Success(_) => Failure(new Error(s"Server exited early"))
      case failure => failure
    }

    try {
      if (ignoreEarlyExit) {
        Await.result(testResult, timeout.toScala)
      } else {
        Await.result(Future.firstCompletedOf(Seq(testResult, serverClosed)), timeout.toScala)
      }
    } catch {
      case e: ExecutionException =>
        throw e.getCause
    } finally {
      session.close()
      session.serverClosed.foreach { case SessionResult(exitCode, stderr) =>
        println(s"Server exited with code $exitCode and stderr:\n$stderr")
      }
    }
  }

  def withLifetime(initializeParams: InitializeBuildParams, session: Session)(
    f: BuildServerCapabilities => Future[Unit]
  )(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      initializeResult <- session.server.buildInitialize(initializeParams).asScala
      _ = session.server.onBuildInitialized()
      _ <- f(initializeResult.getCapabilities)
      _ <- session.server.buildShutdown().asScala
      _ = session.server.onBuildExit()
    } yield ()
  }
}

class TestClient(workspacePath: Path, initializeParams: InitializeBuildParams) {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  private def test(timeout: Duration, ignoreEarlyExit: Boolean = false)(
    test: Session => Future[Unit]
  ): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    withSession(workspacePath, timeout, ignoreEarlyExit) { session =>
      withLifetime(initializeParams, session) { _ =>
        test(session)
      }
    }
  }

  private def assertJsonEquals[T](expected: T, actual: T): Unit = {
    assertEquals(gson.toJson(expected), gson.toJson(actual))
  }

  def testJavacOptions(timeout: Duration)(params: JavacOptionsParams, expectedResult: JavacOptionsResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.buildTargetJavacOptions(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testScalacOptions(timeout: Duration)(params: ScalacOptionsParams, expectedResult: ScalacOptionsResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.buildTargetScalacOptions(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testWorkspaceTargets(timeout: Duration)(expectedResult: WorkspaceBuildTargetsResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.workspaceBuildTargets().asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testCppOptions(timeout: Duration)(params: CppOptionsParams, expectedResult: CppOptionsResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.buildTargetCppOptions(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testSources(timeout: Duration)(params: SourcesParams, expectedResult: SourcesResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.buildTargetSources(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testResources(timeout: Duration)(params: ResourcesParams, expectedResult: ResourcesResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.buildTargetResources(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testInverseSources(timeout: Duration)(params: InverseSourcesParams, expectedResult: InverseSourcesResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.buildTargetInverseSources(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testScalaMainClasses(timeout: Duration)(params: ScalaMainClassesParams, expectedResult: ScalaMainClassesResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.buildTargetScalaMainClasses(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testScalaTestClasses(timeout: Duration)(params: ScalaTestClassesParams, expectedResult: ScalaTestClassesResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.buildTargetScalaTestClasses(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testDependencySources(timeout: Duration)(params: DependencySourcesParams, expectedResult: DependencySourcesResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.buildTargetDependencySources(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testJvmRunEnvironment(timeout: Duration)(params: JvmRunEnvironmentParams, expectedResult: JvmRunEnvironmentResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.jvmRunEnvironment(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }

  def testJvmTestEnvironment(timeout: Duration)(params: JvmTestEnvironmentParams, expectedResult: JvmTestEnvironmentResult): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      session.server.jvmTestEnvironment(params).asScala.map { result =>
        assertJsonEquals(expectedResult, result)
      }
    }
  }


  /**
   * Simulates a typical workflow
   */
  def testResolveProject(timeout: Duration): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    test(timeout) { session =>
      val getWorkspaceTargets = session.server.workspaceBuildTargets().asScala
        .map(targetsResult => targetsResult.getTargets)

      val extractTargetIdsForLanguage = (targets: java.util.List[BuildTarget], languageId: String) => targets.asScala.filter(_.getLanguageIds.contains(languageId)).map(_.getId).toList.asJava

      val extractTargetIds = (targets: java.util.List[BuildTarget]) =>
        targets.asScala.map(_.getId).toList.asJava

      val getSources = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetSources(new SourcesParams(targetIds)).asScala
      val getResources = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetResources(new ResourcesParams(targetIds)).asScala
      val getJavacOptions = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetJavacOptions(new JavacOptionsParams(targetIds)).asScala
      val getScalacOptions = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetScalacOptions(new ScalacOptionsParams(targetIds)).asScala
      val getCppOptions = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetCppOptions(new CppOptionsParams(targetIds)).asScala


      for {
        targets <- getWorkspaceTargets
        targetIds = extractTargetIds(targets)
        sources <- getSources(targetIds)
        resources <- getResources(targetIds)
        javaTargetIds = extractTargetIdsForLanguage(targets, "java") // TODO: use a constant
        javacOptions <- getJavacOptions(javaTargetIds)
        scalaTargetIds = extractTargetIdsForLanguage(targets, "scala") // TODO: use a constant
        scalacOptions <- getScalacOptions(scalaTargetIds)
        cppTargetIds = extractTargetIdsForLanguage(targets, "cpp") // TODO: use a constant
        cppOptions <- getCppOptions(cppTargetIds)
      } yield ()
    }
  }
}
