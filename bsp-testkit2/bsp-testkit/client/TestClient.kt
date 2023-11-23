package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.time.withTimeout
import org.jetbrains.bsp.testkit.JsonComparator
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

suspend fun withSession(
  workspace: Path,
  timeout: Duration,
  ignoreEarlyExit: Boolean = false,
  test: suspend (Session) -> Unit
) {
  coroutineScope {
    val session = Session(workspace, this)
    val testResult = this.async { test(session) }
    try {
      withTimeout(timeout) {
        if (ignoreEarlyExit) {
          testResult.await()
        } else {
          select {
            testResult.onAwait {}
            session.serverClosed.onAwait {}
          }
        }
      }
    } catch (e: ExecutionException) {
      throw e.cause ?: e
    } finally {
      session.close()
      val result = session.serverClosed.await()
      println("Server exited with code ${result.exitCode} and stderr:\n${result.stderr}")
    }
  }
}

suspend fun withLifetime(
  initializeParams: InitializeBuildParams,
  session: Session,
  f: suspend (BuildServerCapabilities) -> Unit
) {
  val initializeResult = session.server.buildInitialize(initializeParams).await()
  session.server.onBuildInitialized()
  f(initializeResult.capabilities)
  session.server.buildShutdown().await()
  session.server.onBuildExit()
}

open class TestClient(open val workspacePath: Path, open val initializeParams: InitializeBuildParams, val transformJson: (String) -> String) {
  val gson = Gson()

  private inline fun <reified T> applyJsonTransform(element: T): T {
    val json = gson.toJson(element)
    val transformed = transformJson(json)
    return gson.fromJson(transformed, T::class.java)
  }

  private inline fun <reified T> assertJsonEquals(expected: T, actual: T) {
    val transformedExpected = applyJsonTransform(expected)
    val transformedActual = applyJsonTransform(actual)
    JsonComparator.assertJsonEquals(transformedExpected, transformedActual, T::class.java)
  }

//  fun testJavacOptions(timeout: Duration, params: JavacOptionsParams, expectedResult: JavacOptionsResult) {
//    val transformedParams = applyJsonTransform(params)
//    withS(timeout) { session ->
//      session.server.buildTargetJavacOptions(transformedParams).thenApply { result ->
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
}


//
//class TestClient(val workspacePath: Path, val initializeParams: InitializeBuildParams, val transformJson: String => String) {
//
//  val gson = new Gson()
//
//  private def test(timeout: Duration, ignoreEarlyExit: Boolean = false)(
//    test: Session => Future[Unit]
//  ): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    withSession(Paths.get(initializeParams.getRootUri), timeout, ignoreEarlyExit) { session =>
//      withLifetime(initializeParams, session) { _ =>
//        test(session)
//      }
//    }
//  }
//
//  private def applyJsonTransform[T](element: T, typeOfT: Type): T = {
//    val json = gson.toJson(element, typeOfT)
//    val transformed = transformJson(json)
//    gson.fromJson[T](transformed, typeOfT)
//  }
//
//  private def assertJsonEquals[T](expected: T, actual: T): Unit = {
//    val typeOfT = new TypeToken[T] {}.getType
//    val transformedExpected = applyJsonTransform(expected, typeOfT)
//    val transformedActual = applyJsonTransform(actual, typeOfT)
//    JsonComparator.assertJsonEquals(transformedExpected, transformedActual, typeOfT)
//  }
//
//  def testJavacOptions(timeout: Duration)(params: JavacOptionsParams, expectedResult: JavacOptionsResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[JavacOptionsParams] {}.getType
//    val transformedParams = applyJsonTransform[JavacOptionsParams](params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetJavacOptions(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testScalacOptions(timeout: Duration)(params: ScalacOptionsParams, expectedResult: ScalacOptionsResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[ScalacOptionsParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetScalacOptions(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testWorkspaceTargets(timeout: Duration)(expectedResult: WorkspaceBuildTargetsResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    test(timeout) { session =>
//      session.server.workspaceBuildTargets().asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testCppOptions(timeout: Duration)(params: CppOptionsParams, expectedResult: CppOptionsResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[CppOptionsParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetCppOptions(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testPythonOptions(timeout: Duration)(params: PythonOptionsParams, expectedResult: PythonOptionsResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[PythonOptionsParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetPythonOptions(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testSources(timeout: Duration)(params: SourcesParams, expectedResult: SourcesResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[SourcesParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetSources(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testResources(timeout: Duration)(params: ResourcesParams, expectedResult: ResourcesResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[ResourcesParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetResources(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testInverseSources(timeout: Duration)(params: InverseSourcesParams, expectedResult: InverseSourcesResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[InverseSourcesParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetInverseSources(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testScalaMainClasses(timeout: Duration)(params: ScalaMainClassesParams, expectedResult: ScalaMainClassesResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[ScalaMainClassesParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetScalaMainClasses(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testScalaTestClasses(timeout: Duration)(params: ScalaTestClassesParams, expectedResult: ScalaTestClassesResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[ScalaTestClassesParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetScalaTestClasses(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testDependencySources(timeout: Duration)(params: DependencySourcesParams, expectedResult: DependencySourcesResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[DependencySourcesParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetDependencySources(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testJvmRunEnvironment(timeout: Duration)(params: JvmRunEnvironmentParams, expectedResult: JvmRunEnvironmentResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[JvmRunEnvironmentParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetJvmRunEnvironment(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  def testJvmTestEnvironment(timeout: Duration)(params: JvmTestEnvironmentParams, expectedResult: JvmTestEnvironmentResult): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    val typeOfT = new TypeToken[JvmTestEnvironmentParams] {}.getType
//    val transformedParams = applyJsonTransform(params, typeOfT)
//    test(timeout) { session =>
//      session.server.buildTargetJvmTestEnvironment(transformedParams).asScala.map { result =>
//        assertJsonEquals(expectedResult, result)
//      }
//    }
//  }
//
//  /**
//   * Simulates a typical workflow
//   */
//  def testResolveProject(timeout: Duration): Unit = {
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    test(timeout) { session =>
//      val getWorkspaceTargets = session.server.workspaceBuildTargets().asScala
//        .map(targetsResult => targetsResult.getTargets)
//
//      val extractTargetIdsForLanguage = (targets: java.util.List[BuildTarget], languageId: String) => targets.asScala.filter(_.getLanguageIds.contains(languageId)).map(_.getId).toList.asJava
//
//      val extractTargetIds = (targets: java.util.List[BuildTarget]) =>
//        targets.asScala.map(_.getId).toList.asJava
//
//      val getSources = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetSources(new SourcesParams(targetIds)).asScala
//      val getResources = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetResources(new ResourcesParams(targetIds)).asScala
//      val getJavacOptions = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetJavacOptions(new JavacOptionsParams(targetIds)).asScala
//      val getScalacOptions = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetScalacOptions(new ScalacOptionsParams(targetIds)).asScala
//      val getCppOptions = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetCppOptions(new CppOptionsParams(targetIds)).asScala
//      val getPythonOptions = (targetIds: java.util.List[BuildTargetIdentifier]) => session.server.buildTargetPythonOptions(new PythonOptionsParams(targetIds)).asScala
//
//      for {
//        targets <- getWorkspaceTargets
//        targetIds = extractTargetIds(targets)
//        sources <- getSources(targetIds)
//        resources <- getResources(targetIds)
//        javaTargetIds = extractTargetIdsForLanguage(targets, "java") // TODO: use a constant
//        javacOptions <- getJavacOptions(javaTargetIds)
//        scalaTargetIds = extractTargetIdsForLanguage(targets, "scala") // TODO: use a constant
//        scalacOptions <- getScalacOptions(scalaTargetIds)
//        cppTargetIds = extractTargetIdsForLanguage(targets, "cpp") // TODO: use a constant
//        cppOptions <- getCppOptions(cppTargetIds)
//        pythonTargetIds = extractTargetIdsForLanguage(targets, "python")
//        pythonOptions <- getPythonOptions(pythonTargetIds)
//      } yield ()
//    }
//  }
//}
