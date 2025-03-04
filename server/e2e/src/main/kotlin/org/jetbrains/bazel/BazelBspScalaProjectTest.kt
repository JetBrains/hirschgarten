package org.jetbrains.bazel

import org.apache.logging.log4j.LogManager
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.Diagnostic
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.Position
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.Range
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.ScalaPlatform
import org.jetbrains.bsp.protocol.ScalacOptionsItem
import org.jetbrains.bsp.protocol.ScalacOptionsParams
import org.jetbrains.bsp.protocol.ScalacOptionsResult
import org.jetbrains.bsp.protocol.StatusCode
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object BazelBspScalaProjectTest : BazelBspTestBaseScenario() {
  private val log = LogManager.getLogger(BazelBspScalaProjectTest::class.java)
  private val testClient = createTestkitClient()

  @JvmStatic
  fun main(args: Array<String>) =
    try {
      executeScenario()
    } catch (t: Throwable) {
      testClient.client.logMessageNotifications.forEach {
        log.info(it.message)
      }
      throw t
    }

  override fun additionalServerInstallArguments() = arrayOf("-enabled-rules", "io_bazel_rules_scala", "rules_java", "rules_jvm")

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      resolveProject(),
      compareWorkspaceTargetsResults(),
      compileWithWarnings(),
      scalaOptionsResults(),
    )

  private fun resolveProject(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "resolve project",
    ) { testClient.testResolveProject(2.minutes) }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHome = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_$javaHomeArchitecture/"
    val jvmBuildTarget =
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = "11",
      )
    val scalaBuildTarget =
      ScalaBuildTarget(
        "org.scala-lang",
        "2.12.14",
        "2.12",
        ScalaPlatform.JVM,
        listOf(
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_compiler/scala-compiler-2.12.14.jar",
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
        ),
        jvmBuildTarget = jvmBuildTarget,
      )

    val target =
      BuildTarget(
        Label.parse("$targetPrefix//scala_targets:library"),
        listOf("library"),
        listOf("scala"),
        listOf(
          Label.parse(Label.synthetic("scala-compiler-2.12.14.jar").toString()),
          Label.parse(Label.synthetic("scala-library-2.12.14.jar").toString()),
          Label.parse(Label.synthetic("scala-reflect-2.12.14.jar").toString()),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "//scala_targets:library",
        baseDirectory = "file://\$WORKSPACE/scala_targets/",
        data = scalaBuildTarget,
      )
    return WorkspaceBuildTargetsResult(
      listOf(target),
    )
  }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) { testClient.testWorkspaceTargets(120.seconds, expectedWorkspaceBuildTargetsResult()) }

  private fun scalaOptionsResults(): BazelBspTestScenarioStep {
    val expectedTargetIdentifiers = expectedTargetIdentifiers().filter { it != Label.synthetic("bsp-workspace-root") }
    val expectedScalaOptionsItems =
      expectedTargetIdentifiers.map {
        ScalacOptionsItem(
          it,
          emptyList(),
          listOf(
            "$bazelBinDirectory/external/io_bazel_rules_scala_scala_library/io_bazel_rules_scala_scala_library.stamp/scala-library-2.12.14-stamped.jar",
            "$bazelBinDirectory/external/io_bazel_rules_scala_scala_reflect/io_bazel_rules_scala_scala_reflect.stamp/scala-reflect-2.12.14-stamped.jar",
          ),
          "$bazelBinDirectory/scala_targets/library.jar",
        )
      }
    val expectedScalaOptionsResult = ScalacOptionsResult(expectedScalaOptionsItems)
    val scalaOptionsParams = ScalacOptionsParams(expectedTargetIdentifiers)
    return BazelBspTestScenarioStep("scalaOptions results") {
      testClient.testScalacOptions(120.seconds, scalaOptionsParams, expectedScalaOptionsResult)
    }
  }

  // All expected diagnostics must be present, but there can be more
  private fun checkDiagnostics(
    params: CompileParams,
    expectedResult: CompileResult,
    expectedDiagnostics: List<PublishDiagnosticsParams>,
  ) {
    val transformedParams = testClient.applyJsonTransform(params)
    testClient.test(60.seconds) { session ->
      session.client.clearDiagnostics()
      val result = session.server.buildTargetCompile(transformedParams)
      expectedDiagnostics.forEach { expected ->
        session.client.publishDiagnosticsNotifications
          .find { actual ->
            actual.originId == expected.originId &&
              actual.buildTarget == expected.buildTarget &&
              actual.textDocument == expected.textDocument
          }?.let {
            testClient.assertJsonEquals(expected, it)
          } ?: error("Expected diagnostics $expected not found. Actual diagnostics: ${session.client.publishDiagnosticsNotifications}")
      }
      testClient.assertJsonEquals(expectedResult, result)
    }
  }

  private fun compileWithWarnings(): BazelBspTestScenarioStep {
    val expectedTargetIdentifiers = expectedTargetIdentifiers().filter { it != Label.synthetic("bsp-workspace-root") }
    val compileParams = CompileParams(expectedTargetIdentifiers, originId = UUID.randomUUID().toString())

    val expectedCompilerResult = CompileResult(StatusCode.OK)
    val expectedDiagnostic =
      Diagnostic(
        Range(Position(4, 2), Position(4, 2)),
        "match may not be exhaustive.\nIt would fail on the following input: C(_)\n  aa match {\n  ^",
        severity = DiagnosticSeverity.WARNING,
      )

    val tmpDir = System.getenv()["BIT_WORKSPACE_DIR"]
    val expectedDocumentId = TextDocumentIdentifier("file://$tmpDir/scala_targets/Example.scala")
    val expectedDiagnosticsParam =
      PublishDiagnosticsParams(
        expectedDocumentId,
        expectedTargetIdentifiers[0],
        diagnostics = arrayListOf(expectedDiagnostic),
        reset = true,
        originId = compileParams.originId,
      )

    return BazelBspTestScenarioStep("compile results") {
      checkDiagnostics(compileParams, expectedCompilerResult, listOf(expectedDiagnosticsParam))
      // The second build call publishes no diagnostics. Diagnostics are supposed to be cached on the client side.
      checkDiagnostics(compileParams, expectedCompilerResult, listOf())
    }
  }
}
