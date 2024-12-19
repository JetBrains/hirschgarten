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

object JavaDiagnosticsTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    TODO("Not needed for this test")
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      `depreacted warning target gives two warnings`(),
      `building two targets gives correct diagnostics`(),
    )

  val expectedUnknownFileUri = "file://$workspaceDir/%3Cunknown%3E"
  val expectedDeprecatedWarningMessage =
    "[removal] deprecatedMethod() in Library has been deprecated and marked for removal\n    Library.deprecatedMethod();\n           ^"
  val expectedJavacWarningMessage =
    "warning: [options] source value 8 is obsolete and will be removed in a future release\nwarning: [options] target value 8 is obsolete and will be removed in a future release\nwarning: [options] To suppress warnings about obsolete options, use -Xlint:-options."

  private fun `depreacted warning target gives two warnings`(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "deprecated warning",
    ) {
      val currentTime = System.currentTimeMillis()
      val targetUri = "@//:deprecated_warning"
      val params =
        CompileParams(listOf(BuildTargetIdentifier(targetUri))).apply {
          originId = "some-id"
          arguments = listOf("--action_env=FORCE_REBUILD=$currentTime")
        }
      val transformedParams = testClient.applyJsonTransform(params)

      val expectedDeprecatedWarningFileUri = "file://$workspaceDir/DeprecatedWarning.java"
      testClient.test(60.seconds) { session, _ ->
        session.client.clearDiagnostics()
        val result = session.server.buildTargetCompile(transformedParams).await()
        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(params.originId, result.originId)
        assertNull(result.data)
        assertNull(result.dataKind)
        println(session.client.publishDiagnosticsNotifications)
        assertEquals(1, session.client.publishDiagnosticsNotifications.size)
        val deprecatedWarning =
          session.client.publishDiagnosticsNotifications.find {
            it.textDocument.uri == expectedDeprecatedWarningFileUri
          }!!

        assertEquals(1, deprecatedWarning.diagnostics.size)
        assertEquals(expectedDeprecatedWarningMessage, deprecatedWarning.diagnostics[0].message)
        assertEquals(
          2,
          deprecatedWarning.diagnostics[0]
            .range.start.line,
        )
        assertEquals(
          11,
          deprecatedWarning.diagnostics[0]
            .range.start.character,
        )
        assertEquals(
          2,
          deprecatedWarning.diagnostics[0]
            .range.end.line,
        )
        assertEquals(
          11,
          deprecatedWarning.diagnostics[0]
            .range.end.character,
        )
        assertEquals(true, deprecatedWarning.reset)
        assertEquals(params.originId, deprecatedWarning.originId)
        assertEquals(targetUri, deprecatedWarning.buildTarget.uri)
        assertEquals(DiagnosticSeverity.WARNING, deprecatedWarning.diagnostics[0].severity)
        assertNull(deprecatedWarning.diagnostics[0].code)
        assertNull(deprecatedWarning.diagnostics[0].codeDescription)
        assertNull(deprecatedWarning.diagnostics[0].source)
        assertNull(deprecatedWarning.diagnostics[0].tags)
        assertNull(deprecatedWarning.diagnostics[0].relatedInformation)
        assertNull(deprecatedWarning.diagnostics[0].dataKind)
        assertNull(deprecatedWarning.diagnostics[0].data)
      }
    }

  private fun `building two targets gives correct diagnostics`(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "no such method error",
    ) {
      val currentTime = System.currentTimeMillis()
      val noSuchMethodTargetUri = "@//:no_such_method_error"
      val warningAndErrorTargetUri = "@//:warning_and_error"
      val params =
        CompileParams(
          listOf(
            BuildTargetIdentifier(noSuchMethodTargetUri),
            BuildTargetIdentifier(warningAndErrorTargetUri),
          ),
        ).apply {
          originId = "some-id"
          arguments = listOf("--action_env=FORCE_REBUILD=$currentTime", "--keep_going")
        }
      val transformedParams = testClient.applyJsonTransform(params)

      val expectedNoSuchMethodErrorFileUri = "file://$workspaceDir/NoSuchMethodError.java"
      val expectedWarningAndErrorFileUri = "file://$workspaceDir/WarningAndError.java"

      testClient.test(60.seconds) { session, _ ->
        session.client.clearDiagnostics()
        val result = session.server.buildTargetCompile(transformedParams).await()
        println(session.client.logMessageNotifications)
        assertEquals(StatusCode.ERROR, result.statusCode)
        assertEquals(params.originId, result.originId)
        assertNull(result.data)
        assertNull(result.dataKind)
        assertEquals(2, session.client.publishDiagnosticsNotifications.size)
        val noSuchMethodError =
          session.client.publishDiagnosticsNotifications.find {
            it.textDocument.uri == expectedNoSuchMethodErrorFileUri
          }!!
        val warningAndError =
          session.client.publishDiagnosticsNotifications.find {
            it.textDocument.uri == expectedWarningAndErrorFileUri
          }!!

        assertEquals(true, noSuchMethodError.reset)
        assertEquals(params.originId, noSuchMethodError.originId)
        assertEquals(1, noSuchMethodError.diagnostics.size)
        assertEquals(expectedNoSuchMethodErrorFileUri, noSuchMethodError.textDocument.uri)
        assertEquals(
          "cannot find symbol\n    noSuchMethod();\n    ^\n  symbol:   method noSuchMethod()\n  location: class NoSuchMethodError",
          noSuchMethodError.diagnostics[0].message,
        )
        assertEquals(
          2,
          noSuchMethodError.diagnostics[0]
            .range.start.line,
        )
        assertEquals(
          4,
          noSuchMethodError.diagnostics[0]
            .range.start.character,
        )
        assertEquals(
          2,
          noSuchMethodError.diagnostics[0]
            .range.end.line,
        )
        assertEquals(
          4,
          noSuchMethodError.diagnostics[0]
            .range.end.character,
        )
        assertEquals(true, noSuchMethodError.reset)
        assertEquals(params.originId, noSuchMethodError.originId)
        assertEquals(noSuchMethodTargetUri, noSuchMethodError.buildTarget.uri)
        assertEquals(DiagnosticSeverity.ERROR, noSuchMethodError.diagnostics[0].severity)
        assertNull(noSuchMethodError.diagnostics[0].code)
        assertNull(noSuchMethodError.diagnostics[0].codeDescription)
        assertNull(noSuchMethodError.diagnostics[0].source)
        assertNull(noSuchMethodError.diagnostics[0].tags)
        assertNull(noSuchMethodError.diagnostics[0].relatedInformation)
        assertNull(noSuchMethodError.diagnostics[0].dataKind)
        assertNull(noSuchMethodError.diagnostics[0].data)

        assertEquals(true, warningAndError.reset)
        assertEquals(params.originId, warningAndError.originId)
        assertEquals(2, warningAndError.diagnostics.size)
        assertEquals(expectedWarningAndErrorFileUri, warningAndError.textDocument.uri)
        val errorDiagnostic = warningAndError.diagnostics.find { it.severity == DiagnosticSeverity.ERROR }!!
        assertEquals(
          "cannot find symbol\n    noSuchMethod();\n    ^\n  symbol:   method noSuchMethod()\n  location: class WarningAndError",
          errorDiagnostic.message,
        )
        assertEquals(3, errorDiagnostic.range.start.line)
        assertEquals(4, errorDiagnostic.range.start.character)
        assertEquals(3, errorDiagnostic.range.end.line)
        assertEquals(4, errorDiagnostic.range.end.character)
        assertEquals(DiagnosticSeverity.ERROR, errorDiagnostic.severity)
        assertNull(errorDiagnostic.code)
        assertNull(errorDiagnostic.codeDescription)
        assertNull(errorDiagnostic.source)
        assertNull(errorDiagnostic.tags)
        assertNull(errorDiagnostic.relatedInformation)
        assertNull(errorDiagnostic.dataKind)
        assertNull(errorDiagnostic.data)
        val warningDiagnostic = warningAndError.diagnostics.find { it.severity == DiagnosticSeverity.WARNING }!!
        assertEquals(expectedDeprecatedWarningMessage, warningDiagnostic.message)
        assertEquals(2, warningDiagnostic.range.start.line)
        assertEquals(11, warningDiagnostic.range.start.character)
        assertEquals(2, warningDiagnostic.range.end.line)
        assertEquals(11, warningDiagnostic.range.end.character)
        assertEquals(DiagnosticSeverity.WARNING, warningDiagnostic.severity)
        assertNull(warningDiagnostic.code)
        assertNull(warningDiagnostic.codeDescription)
        assertNull(warningDiagnostic.source)
        assertNull(warningDiagnostic.tags)
        assertNull(warningDiagnostic.relatedInformation)
        assertNull(warningDiagnostic.dataKind)
        assertNull(warningDiagnostic.data)
      }
    }
}
