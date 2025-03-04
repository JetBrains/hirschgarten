package org.jetbrains.bsp.testkit.client

import kotlinx.coroutines.test.runTest
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.CppOptionsParams
import org.jetbrains.bsp.protocol.CppOptionsResult
import org.jetbrains.bsp.protocol.DependencyModulesParams
import org.jetbrains.bsp.protocol.DependencyModulesResult
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.InitializeBuildParams
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JvmCompileClasspathParams
import org.jetbrains.bsp.protocol.JvmCompileClasspathResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.RustWorkspaceParams
import org.jetbrains.bsp.protocol.RustWorkspaceResult
import org.jetbrains.bsp.protocol.ScalacOptionsParams
import org.jetbrains.bsp.protocol.ScalacOptionsResult
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.testkit.JsonComparator
import org.jetbrains.bsp.testkit.gsonSealedSupport
import java.nio.file.Path
import kotlin.time.Duration

suspend fun withSession(
  workspace: Path,
  client: MockClient,
  test: suspend (Session) -> Unit,
) {
  val session = Session(workspace, client)
  test(session)
}

suspend fun withLifetime(
  initializeParams: InitializeBuildParams,
  session: Session,
  f: suspend () -> Unit,
) {
  session.server.buildInitialize(initializeParams)
  session.server.onBuildInitialized()
  f()
  session.server.buildShutdown()
  session.server.onBuildExit()
}

open class BasicTestClient(
  val workspacePath: Path,
  val initializeParams: InitializeBuildParams,
  val transformJson: (String) -> String,
  val client: MockClient,
) {
  val gson = gsonSealedSupport

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
      withSession(workspacePath, client) { session ->
        withLifetime(initializeParams, session) {
          doTest(session)
        }
      }
    }
  }
}

class TestClient(
  workspacePath: Path,
  initializeParams: InitializeBuildParams,
  transformJson: (String) -> String,
) : BasicTestClient(
    workspacePath,
    initializeParams,
    transformJson,
    MockClient(),
  ) {
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

  fun testRustWorkspace(
    timeout: Duration,
    params: RustWorkspaceParams,
    expectedResult: RustWorkspaceResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.rustWorkspace(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testSources(
    timeout: Duration,
    params: SourcesParams,
    expectedResult: SourcesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetSources(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testResources(
    timeout: Duration,
    params: ResourcesParams,
    expectedResult: ResourcesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetResources(transformedParams)
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

  fun testJvmCompileClasspath(
    timeout: Duration,
    params: JvmCompileClasspathParams,
    expectedResult: JvmCompileClasspathResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetJvmCompileClasspath(transformedParams)
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testDependencyModule(
    timeout: Duration,
    params: DependencyModulesParams,
    expectedResult: DependencyModulesResult,
  ) {
    val transformedParams = applyJsonTransform(params)
    test(timeout) { session ->
      val result = session.server.buildTargetDependencyModules(transformedParams)
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
        session.server.buildTargetSources(SourcesParams(targetIds))
        session.server.buildTargetResources(ResourcesParams(targetIds))
        val javaTargetIds = targets.filter { it.languageIds.contains("java") }.map { it.id }
        session.server.buildTargetJavacOptions(JavacOptionsParams(javaTargetIds))
        val scalaTargetIds = targets.filter { it.languageIds.contains("scala") }.map { it.id }
        session.server.buildTargetScalacOptions(ScalacOptionsParams(scalaTargetIds))
        val cppTargetIds = targets.filter { it.languageIds.contains("cpp") }.map { it.id }
        session.server.buildTargetCppOptions(CppOptionsParams(cppTargetIds))
        val pythonTargetIds = targets.filter { it.languageIds.contains("python") }.map { it.id }
        session.server.buildTargetPythonOptions(PythonOptionsParams(pythonTargetIds))
        val rustTargetIds = targets.filter { it.languageIds.contains("rust") }.map { it.id }
        session.server.rustWorkspace(RustWorkspaceParams(rustTargetIds))
      }
    }
  }
}
