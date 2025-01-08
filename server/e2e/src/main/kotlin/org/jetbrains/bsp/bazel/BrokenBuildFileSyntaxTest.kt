package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.DiagnosticSeverity
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import kotlin.time.Duration.Companion.seconds

// ERROR: /home/andrzej.gluszak/code/jetbrains/hirschgarten/server/e2e/test-projects/bzlmod/broken-syntax-in-build-file/BUILD.bazel:2:11: 'is' not supported, use '==' instead
//ERROR: /home/andrzej.gluszak/code/jetbrains/hirschgarten/server/e2e/test-projects/bzlmod/broken-syntax-in-build-file/BUILD.bazel:2:11: syntax error at 'is': expected ,
//ERROR: /home/andrzej.gluszak/code/jetbrains/hirschgarten/server/e2e/test-projects/bzlmod/broken-syntax-in-build-file/BUILD.bazel:6:1: syntax error at 'newline': expected expression
//ERROR: package contains errors: server/e2e/test-projects/bzlmod/broken-syntax-in-build-file
//ERROR: package contains errors: server/e2e/test-projects/bzlmod/broken-syntax-in-build-file: 'is' not supported, use '==' instead

object BrokenBuildFileSyntaxTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    TODO("Not needed for this test")
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      `broken syntax is reported`(),
    )

  val expectedUnknownFileUri = "file://$workspaceDir/%3Cunknown%3E"
  val expectedDeprecatedWarningMessage =
    "[removal] deprecatedMethod() in Library has been deprecated and marked for removal\n    Library.deprecatedMethod();\n           ^"
  val expectedJavacWarningMessage =
    "warning: [options] source value 8 is obsolete and will be removed in a future release\nwarning: [options] target value 8 is obsolete and will be removed in a future release\nwarning: [options] To suppress warnings about obsolete options, use -Xlint:-options."

  private fun `broken syntax is reported`(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "broken syntax",
    ) {
      val targetUri = "@//..."
      val params =
        CompileParams(listOf(BuildTargetIdentifier(targetUri))).apply {
          originId = "some-id"
        }
      val transformedParams = testClient.applyJsonTransform(params)

      val expectedDeprecatedWarningFileUri = "file://$workspaceDir/BUILD.bazel"
      testClient.test(20.seconds) { session, _ ->
        session.client.clearDiagnostics()
        val result = session.server.buildTargetCompile(transformedParams).await()
        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(params.originId, result.originId)
        assertNull(result.data)
        assertNull(result.dataKind)
        println(session.client.publishDiagnosticsNotifications)
        assertEquals(1, session.client.publishDiagnosticsNotifications.size)
//        val deprecatedWarning =
//          session.client.publishDiagnosticsNotifications.find {
//            it.textDocument.uri == expectedDeprecatedWarningFileUri
//          }!!
//
//        assertEquals(1, deprecatedWarning.diagnostics.size)
//        assertEquals(expectedDeprecatedWarningMessage, deprecatedWarning.diagnostics[0].message)
//        assertEquals(
//          2,
//          deprecatedWarning.diagnostics[0]
//            .range.start.line,
//        )
//        assertEquals(
//          11,
//          deprecatedWarning.diagnostics[0]
//            .range.start.character,
//        )
//        assertEquals(
//          2,
//          deprecatedWarning.diagnostics[0]
//            .range.end.line,
//        )
//        assertEquals(
//          11,
//          deprecatedWarning.diagnostics[0]
//            .range.end.character,
//        )
//        assertEquals(true, deprecatedWarning.reset)
//        assertEquals(params.originId, deprecatedWarning.originId)
//        assertEquals(targetUri, deprecatedWarning.buildTarget.uri)
//        assertEquals(DiagnosticSeverity.WARNING, deprecatedWarning.diagnostics[0].severity)
//        assertNull(deprecatedWarning.diagnostics[0].code)
//        assertNull(deprecatedWarning.diagnostics[0].codeDescription)
//        assertNull(deprecatedWarning.diagnostics[0].source)
//        assertNull(deprecatedWarning.diagnostics[0].tags)
//        assertNull(deprecatedWarning.diagnostics[0].relatedInformation)
//        assertNull(deprecatedWarning.diagnostics[0].dataKind)
//        assertNull(deprecatedWarning.diagnostics[0].data)
      }
    }
}
