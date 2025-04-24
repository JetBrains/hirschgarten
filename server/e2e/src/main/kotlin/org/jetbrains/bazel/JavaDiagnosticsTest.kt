package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import kotlin.io.path.Path
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

  val expectedUnknownFileUri = Path("$workspaceDir/%3Cunknown%3E")
  val expectedDeprecatedWarningMessage =
    "[removal] deprecatedMethod() in Library has been deprecated and marked for removal\n    Library.deprecatedMethod();\n           ^"
  val expectedJavacWarningMessage =
    "warning: [options] source value 8 is obsolete and will be removed in a future release\nwarning: [options] target value 8 is obsolete and will be removed in a future release\nwarning: [options] To suppress warnings about obsolete options, use -Xlint:-options."

  private fun `depreacted warning target gives two warnings`(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "deprecated warning",
    ) {
      val currentTime = System.currentTimeMillis()
      val targetUri = Label.parse("@//:deprecated_warning")
      val params =
        CompileParams(
          listOf(targetUri),
          originId = "some-id",
          arguments = listOf("--action_env=FORCE_REBUILD=$currentTime"),
        )
      val transformedParams = testClient.applyJsonTransform(params)

      val expectedDeprecatedWarningFileUri = Path("$workspaceDir/DeprecatedWarning.java")
      testClient.test(60.seconds) { session ->
        session.client.clearDiagnostics()
        val result = session.server.buildTargetCompile(transformedParams)
        assertEquals(BazelStatus.SUCCESS, result.statusCode)
        println(session.client.publishDiagnosticsNotifications)
        assertEquals(1, session.client.publishDiagnosticsNotifications.size)
        val deprecatedWarning =
          session.client.publishDiagnosticsNotifications.find {
            it.textDocument?.path == expectedDeprecatedWarningFileUri
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
        assertEquals(targetUri, deprecatedWarning.buildTarget)
        assertEquals(DiagnosticSeverity.WARNING, deprecatedWarning.diagnostics[0].severity)
        assertNull(deprecatedWarning.diagnostics[0].code)
        assertNull(deprecatedWarning.diagnostics[0].codeDescription)
        assertNull(deprecatedWarning.diagnostics[0].source)
        assertNull(deprecatedWarning.diagnostics[0].tags)
        assertNull(deprecatedWarning.diagnostics[0].relatedInformation)
      }
    }

  private fun `building two targets gives correct diagnostics`(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "no such method error",
    ) {
      val currentTime = System.currentTimeMillis()
      val noSuchMethodTargetUri = Label.parse("@//:no_such_method_error")
      val warningAndErrorTargetUri = Label.parse("@//:warning_and_error")
      val params =
        CompileParams(
          listOf(
            noSuchMethodTargetUri,
            warningAndErrorTargetUri,
          ),
          originId = "some-id",
          arguments = listOf("--action_env=FORCE_REBUILD=$currentTime", "--keep_going"),
        )
      val transformedParams = testClient.applyJsonTransform(params)

      val expectedNoSuchMethodErrorFileUri = Path("$workspaceDir/NoSuchMethodError.java")
      val expectedWarningAndErrorFileUri = Path("$workspaceDir/WarningAndError.java")

      testClient.test(60.seconds) { session ->
        session.client.clearDiagnostics()
        val result = session.server.buildTargetCompile(transformedParams)
        println(session.client.logMessageNotifications)
        assertEquals(BazelStatus.FATAL_ERROR, result.statusCode)
        assertEquals(2, session.client.publishDiagnosticsNotifications.size)
        val noSuchMethodError =
          session.client.publishDiagnosticsNotifications.find {
            it.textDocument?.path == expectedNoSuchMethodErrorFileUri
          }!!
        val warningAndError =
          session.client.publishDiagnosticsNotifications.find {
            it.textDocument?.path == expectedWarningAndErrorFileUri
          }!!

        assertEquals(true, noSuchMethodError.reset)
        assertEquals(params.originId, noSuchMethodError.originId)
        assertEquals(1, noSuchMethodError.diagnostics.size)
        assertEquals(expectedNoSuchMethodErrorFileUri, noSuchMethodError.textDocument?.path)
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
        assertEquals(noSuchMethodTargetUri, noSuchMethodError.buildTarget)
        assertEquals(DiagnosticSeverity.ERROR, noSuchMethodError.diagnostics[0].severity)
        assertNull(noSuchMethodError.diagnostics[0].code)
        assertNull(noSuchMethodError.diagnostics[0].codeDescription)
        assertNull(noSuchMethodError.diagnostics[0].source)
        assertNull(noSuchMethodError.diagnostics[0].tags)
        assertNull(noSuchMethodError.diagnostics[0].relatedInformation)

        assertEquals(true, warningAndError.reset)
        assertEquals(params.originId, warningAndError.originId)
        assertEquals(2, warningAndError.diagnostics.size)
        assertEquals(expectedWarningAndErrorFileUri, warningAndError.textDocument?.path)
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
      }
    }
}
