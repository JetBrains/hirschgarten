package org.jetbrains.bsp.testkit.client

import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.server.connection.startServer
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceNameResult
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
      val result =
        session.server.workspaceBuildTargets(WorkspaceBuildTargetParams(WorkspaceBuildTargetSelector.AllTargets))
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testMainWorkspaceTargets(timeout: Duration, expectedResult: WorkspaceBuildTargetsResult) {
    test(timeout) { session ->
      val result = session.server.workspaceBuildTargets(WorkspaceBuildTargetParams(WorkspaceBuildTargetSelector.AllTargets))
      // There was some filtering
      assertJsonEquals(expectedResult, result)
    }
  }

  fun testWorkspaceName(timeout: Duration, expectedResult: WorkspaceNameResult) {
    test(timeout) { session ->
      val result = session.server.workspaceName()
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
}
