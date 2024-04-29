package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.CppOptionsParams
import ch.epfl.scala.bsp4j.CppOptionsResult
import ch.epfl.scala.bsp4j.DependencyModulesParams
import ch.epfl.scala.bsp4j.DependencyModulesResult
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.JavacOptionsResult
import ch.epfl.scala.bsp4j.JvmCompileClasspathParams
import ch.epfl.scala.bsp4j.JvmCompileClasspathResult
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.PythonOptionsResult
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.RustWorkspaceParams
import ch.epfl.scala.bsp4j.RustWorkspaceResult
import ch.epfl.scala.bsp4j.ScalaMainClassesParams
import ch.epfl.scala.bsp4j.ScalaMainClassesResult
import ch.epfl.scala.bsp4j.ScalaTestClassesParams
import ch.epfl.scala.bsp4j.ScalaTestClassesResult
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import ch.epfl.scala.bsp4j.ScalacOptionsResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.jetbrains.bsp.testkit.JsonComparator
import org.junit.jupiter.api.Assertions.assertIterableEquals
import java.nio.file.Path
import kotlin.time.Duration

suspend fun <Server : BuildServer, Client : BuildClient> withSession(
  workspace: Path,
  ignoreEarlyExit: Boolean = false,
  withShutdown: Boolean = true,
  client: Client,
  serverClass: Class<Server>,
  test: suspend (Session<Server, Client>) -> Unit,
) = coroutineScope {
  val session = Session(workspace, client, serverClass)
  session.use {
    val testResult = this.async { test(session) }

    if (ignoreEarlyExit) {
      testResult.await()
    } else {
      awaitAll(testResult, session.serverClosed)
      println("selected")
    }
  }

  if (withShutdown) {
    val result = session.serverClosed.await()
    println("Server exited with code ${result.exitCode} and stderr:\n${result.stderr}")
  }
}


suspend fun <Server : BuildServer, Client : BuildClient> withLifetime(
  initializeParams: InitializeBuildParams,
  session: Session<Server, Client>,
  f: suspend (BuildServerCapabilities) -> Unit,
) {
  val initializeResult = session.server.buildInitialize(initializeParams).await()
  session.server.onBuildInitialized()
  f(initializeResult.capabilities)
  session.server.buildShutdown().await()
  session.server.onBuildExit()
}

open class BasicTestClient<Server : BuildServer, Client : BuildClient>(
  val workspacePath: Path,
  val initializeParams: InitializeBuildParams,
  val transformJson: (String) -> String,
  val client: Client,
  val serverClass: Class<Server>,
) {
  val gson = Gson()

  inline fun <reified T> applyJsonTransform(element: T): T {
    val json = gson.toJson(element)
    val transformed = transformJson(json)
    return gson.fromJson(transformed, T::class.java)
  }

  inline fun <reified T> assertJsonEquals(expected: T, actual: T) {
    val transformedExpected = applyJsonTransform(expected)
    val transformedActual = applyJsonTransform(actual)
    JsonComparator.assertJsonEquals(transformedExpected, transformedActual, T::class.java)
  }

  fun test(
    timeout: Duration,
    ignoreEarlyExit: Boolean = false,
    doTest: suspend (Session<Server, Client>, BuildServerCapabilities) -> Unit
  ) {
    runTest(timeout = timeout) {
      withSession(workspacePath, ignoreEarlyExit, true, client, serverClass) { session ->
        withLifetime(initializeParams, session) { capabilities ->
          doTest(session, capabilities)
        }
      }
    }
  }
}

class TestClient(
  workspacePath: Path,
  initializeParams: InitializeBuildParams,
  transformJson: (String) -> String,
) : BasicTestClient<MockServer, MockClient>(
  workspacePath,
  initializeParams,
  transformJson,
  MockClient(),
  MockServer::class.java
) {
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

  fun testCompile(timeout: Duration, params: CompileParams, expectedResult: CompileResult, expectedDiagnostics: List<PublishDiagnosticsParams>) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      session.client.clearDiagnostics()
      val result = session.server.buildTargetCompile(transformedParams).await()
      expectedDiagnostics.zip(session.client.publishDiagnosticsNotifications).forEach{
        assertJsonEquals(it.first, it.second)
      }
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

  fun testRustWorkspace(timeout: Duration, params: RustWorkspaceParams, expectedResult: RustWorkspaceResult) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.rustWorkspace(transformedParams).await()
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

  fun testJvmCompileClasspath(
    timeout: Duration,
    params: JvmCompileClasspathParams,
    expectedResult: JvmCompileClasspathResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetJvmCompileClasspath(transformedParams).await()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testDependencyModule(
    timeout: Duration,
    params: DependencyModulesParams,
    expectedResult: DependencyModulesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session, _ ->
      val result = session.server.buildTargetDependencyModules(transformedParams).await()
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
        val rustTargetIds = targets.filter { it.languageIds.contains("rust") }.map { it.id }
        session.server.rustWorkspace(RustWorkspaceParams(rustTargetIds)).await()
      }
    }
  }
}
