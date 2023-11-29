package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.jetbrains.bsp.testkit.JsonComparator
import java.nio.file.Path
import kotlin.time.Duration

suspend fun withSession(
    workspace: Path,
    ignoreEarlyExit: Boolean = false,
    test: suspend (Session) -> Unit,
) = coroutineScope {
    val session = Session(workspace)
    session.use {
      val testResult = this.async { test(session) }

      if (ignoreEarlyExit) {
        testResult.await()
      } else {
        awaitAll(testResult, session.serverClosed)
        println("selected")
      }
    }

    val result = session.serverClosed.await()
    println("Server exited with code ${result.exitCode} and stderr:\n${result.stderr}")
  }


suspend fun withLifetime(
    initializeParams: InitializeBuildParams,
    session: Session,
    f: suspend (BuildServerCapabilities) -> Unit,
) {
  val initializeResult = session.server.buildInitialize(initializeParams).await()
  session.server.onBuildInitialized()
  f(initializeResult.capabilities)
  session.server.buildShutdown().await()
  session.server.onBuildExit()
}

open class TestClient(
    open val workspacePath: Path,
    open val initializeParams: InitializeBuildParams,
    val transformJson: (String) -> String,
) {
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

  private fun test(timeout: Duration, ignoreEarlyExit: Boolean = false, doTest: suspend (Session, BuildServerCapabilities) -> Unit) {
    runTest(timeout = timeout) {
      withSession(workspacePath, ignoreEarlyExit) { session ->
        withLifetime(initializeParams, session) { capabilities ->
          doTest(session, capabilities)
        }
      }
    }
  }

  fun testJavacOptions(timeout: Duration, params: JavacOptionsParams, expectedResult: JavacOptionsResult) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetJavacOptions(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testScalacOptions(timeout: Duration, params: ScalacOptionsParams, expectedResult: ScalacOptionsResult) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetScalacOptions(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testWorkspaceTargets(timeout: Duration, expectedResult: WorkspaceBuildTargetsResult) {
    test(timeout) { session, _ ->
      val result = session.server.workspaceBuildTargets().await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testCppOptions(timeout: Duration, params: CppOptionsParams, expectedResult: CppOptionsResult) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetCppOptions(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testPythonOptions(timeout: Duration, params: PythonOptionsParams, expectedResult: PythonOptionsResult) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetPythonOptions(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testSources(timeout: Duration, params: SourcesParams, expectedResult: SourcesResult) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetSources(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testResources(timeout: Duration, params: ResourcesParams, expectedResult: ResourcesResult) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetResources(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testInverseSources(
      timeout: Duration,
      params: InverseSourcesParams,
      expectedResult: InverseSourcesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetInverseSources(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  @Suppress("DEPRECATION")
  fun testScalaMainClasses(
      timeout: Duration,
      params: ScalaMainClassesParams,
      expectedResult: ScalaMainClassesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetScalaMainClasses(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  @Suppress("DEPRECATION")
  fun testScalaTestClasses(
      timeout: Duration,
      params: ScalaTestClassesParams,
      expectedResult: ScalaTestClassesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetScalaTestClasses(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testDependencySources(
      timeout: Duration,
      params: DependencySourcesParams,
      expectedResult: DependencySourcesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetDependencySources(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testJvmRunEnvironment(
      timeout: Duration,
      params: JvmRunEnvironmentParams,
      expectedResult: JvmRunEnvironmentResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetJvmRunEnvironment(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testJvmTestEnvironment(
      timeout: Duration,
      params: JvmTestEnvironmentParams,
      expectedResult: JvmTestEnvironmentResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetJvmTestEnvironment(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  /**
   * Simulates a typical workflow
   */
  fun testResolveProject(timeout: Duration) {
    runTest(timeout = timeout) {
      test(timeout) { session, capabilities ->
        val getWorkspaceTargets = session.server.workspaceBuildTargets().await()
        val targets = getWorkspaceTargets.targets
        val targetIds = targets.map { it.id }
        session.server.buildTargetSources(SourcesParams(targetIds)).await()
        if (capabilities.resourcesProvider == true) {
         session.server.buildTargetResources(ResourcesParams(targetIds)).await()
        }
        val javaTargetIds = targets.filter { it.languageIds.contains("java") }.map { it.id }
        session.server.buildTargetJavacOptions(JavacOptionsParams(javaTargetIds)).await()
        val scalaTargetIds = targets.filter { it.languageIds.contains("scala") }.map { it.id }
        session.server.buildTargetScalacOptions(ScalacOptionsParams(scalaTargetIds)).await()
        val cppTargetIds = targets.filter { it.languageIds.contains("cpp") }.map { it.id }
        session.server.buildTargetCppOptions(CppOptionsParams(cppTargetIds)).await()
        val pythonTargetIds = targets.filter { it.languageIds.contains("python") }.map { it.id }
        session.server.buildTargetPythonOptions(PythonOptionsParams(pythonTargetIds)).await()
        // TODO: There should be a provider for this
        //  val rustTargetIds = targets.filter { it.languageIds.contains("rust") }.map { it.id }
        //  val rustWorkspace = session.server.rustWorkspace(RustWorkspaceParams(rustTargetIds)).await()
      }
    }
  }
}
