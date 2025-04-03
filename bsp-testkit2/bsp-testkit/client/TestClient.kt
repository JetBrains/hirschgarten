package org.jetbrains.bsp.testkit.client

import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.server.connection.startServer
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.CppOptionsParams
import org.jetbrains.bsp.protocol.CppOptionsResult
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.ScalacOptionsParams
import org.jetbrains.bsp.protocol.ScalacOptionsResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.testkit.JsonComparator
import java.nio.file.Path
import kotlin.time.Duration

class TestClient(
  val workspacePath: Path,
  val transformJson: (String) -> String,
  val featureFlags: FeatureFlags,
) {
  val gson = bazelGson
  val client = MockClient()

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

  fun test(timeout: Duration, doTest: suspend (Session) -> Unit) {
    runTest(timeout = timeout) {
      val server = startServer(client, workspacePath, null, featureFlags)
      val session = Session(client, server)
      doTest(session)
    }
  }

  fun testJavacOptions(
    timeout: Duration,
    params: JavacOptionsParams,
    expectedResult: JavacOptionsResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetJavacOptions(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testScalacOptions(
    timeout: Duration,
    params: ScalacOptionsParams,
    expectedResult: ScalacOptionsResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetScalacOptions(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testCompile(
    timeout: Duration,
    params: CompileParams,
    expectedResult: CompileResult,
    expectedDiagnostics: List<PublishDiagnosticsParams>,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      session.client.clearDiagnostics()
      val result = session.server.buildTargetCompile(transformedParams)
      expectedDiagnostics.zip(session.client.publishDiagnosticsNotifications).forEach {
        assertJsonEquals(it.first, it.second)
      }
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testWorkspaceTargets(timeout: Duration, expectedResult: WorkspaceBuildTargetsResult) {
    test(timeout) { session ->
      val result = session.server.workspaceBuildTargets()
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testCppOptions(
    timeout: Duration,
    params: CppOptionsParams,
    expectedResult: CppOptionsResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetCppOptions(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testPythonOptions(
    timeout: Duration,
    params: PythonOptionsParams,
    expectedResult: PythonOptionsResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetPythonOptions(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testInverseSources(
    timeout: Duration,
    params: InverseSourcesParams,
    expectedResult: InverseSourcesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetInverseSources(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testDependencySources(
    timeout: Duration,
    params: DependencySourcesParams,
    expectedResult: DependencySourcesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetDependencySources(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testJvmRunEnvironment(
    timeout: Duration,
    params: JvmRunEnvironmentParams,
    expectedResult: JvmRunEnvironmentResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetJvmRunEnvironment(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testJvmTestEnvironment(
    timeout: Duration,
    params: JvmTestEnvironmentParams,
    expectedResult: JvmTestEnvironmentResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetJvmTestEnvironment(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  /**
   * Simulates a typical workflow
   */
  fun testResolveProject(timeout: Duration) {
    runTest(timeout = timeout) {
      test(timeout) { session ->
        val getWorkspaceTargets = session.server.workspaceBuildTargets()
        val targets = getWorkspaceTargets.targets
        val targetIds = targets.map { it.id }
        val javaTargetIds = targets.filter { it.languageIds.contains("java") }.map { it.id }
        session.server.buildTargetJavacOptions(JavacOptionsParams(javaTargetIds))
        val scalaTargetIds = targets.filter { it.languageIds.contains("scala") }.map { it.id }
        session.server.buildTargetScalacOptions(ScalacOptionsParams(scalaTargetIds))
        val cppTargetIds = targets.filter { it.languageIds.contains("cpp") }.map { it.id }
        session.server.buildTargetCppOptions(CppOptionsParams(cppTargetIds))
        val pythonTargetIds = targets.filter { it.languageIds.contains("python") }.map { it.id }
        session.server.buildTargetPythonOptions(PythonOptionsParams(pythonTargetIds))
      }
    }
  }
}
