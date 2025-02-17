package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.Diagnostic
import ch.epfl.scala.bsp4j.DiagnosticSeverity
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.Position
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.Range
import ch.epfl.scala.bsp4j.ScalaBuildTarget
import ch.epfl.scala.bsp4j.ScalaPlatform
import ch.epfl.scala.bsp4j.ScalacOptionsItem
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import ch.epfl.scala.bsp4j.ScalacOptionsResult
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import kotlinx.coroutines.future.await
import org.apache.logging.log4j.LogManager
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object BazelBspScalaProjectTest : BazelBspTestBaseScenario() {
  private val log = LogManager.getLogger(BazelBspScalaProjectTest::class.java)
  private val testClient = createTestkitClient()
  private val testClientClasspathReceiver = createTestkitClient(jvmClasspathReceiver = true)

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
      scalaOptionsResultsClasspathReceiver(),
    )

  private fun resolveProject(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "resolve project",
    ) { testClient.testResolveProject(2.minutes) }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHome = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_$javaHomeArchitecture/"
    val jvmBuildTarget =
      JvmBuildTarget().also {
        it.javaHome = javaHome
        it.javaVersion = "11"
      }
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
      )

    scalaBuildTarget.jvmBuildTarget = jvmBuildTarget

    val target =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//scala_targets:library"),
        listOf("library"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("scala-compiler-2.12.14.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-library-2.12.14.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-reflect-2.12.14.jar").toString()),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    target.displayName = "$targetPrefix//scala_targets:library"
    target.baseDirectory = "file://\$WORKSPACE/scala_targets/"
    target.dataKind = "scala"
    target.data = scalaBuildTarget
    return WorkspaceBuildTargetsResult(
      listOf(target),
    )
  }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) { testClient.testWorkspaceTargets(120.seconds, expectedWorkspaceBuildTargetsResult()) }

  private fun scalaOptionsResults(): BazelBspTestScenarioStep {
    val expectedTargetIdentifiers = expectedTargetIdentifiers().filter { it.uri != "bsp-workspace-root" }
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

  private fun scalaOptionsResultsClasspathReceiver(): BazelBspTestScenarioStep {
    val expectedTargetIdentifiers = expectedTargetIdentifiers().filter { it.uri != "bsp-workspace-root" }
    val expectedScalaOptionsItems =
      expectedTargetIdentifiers.map {
        ScalacOptionsItem(
          it,
          emptyList(),
          emptyList(),
          "$bazelBinDirectory/scala_targets/library.jar",
        )
      }
    val expectedScalaOptionsResult = ScalacOptionsResult(expectedScalaOptionsItems)
    val scalaOptionsParams = ScalacOptionsParams(expectedTargetIdentifiers)
    return BazelBspTestScenarioStep("scalaOptions results") {
      testClientClasspathReceiver.testScalacOptions(60.seconds, scalaOptionsParams, expectedScalaOptionsResult)
    }
  }

  // All expected diagnostics must be present, but there can be more
  private fun checkDiagnostics(
    params: CompileParams,
    expectedResult: CompileResult,
    expectedDiagnostics: List<PublishDiagnosticsParams>,
  ) {
    val transformedParams = testClient.applyJsonTransform(params)
    testClient.test(60.seconds) { session, _ ->
      session.client.clearDiagnostics()
      val result = session.server.buildTargetCompile(transformedParams).await()
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
    val expectedTargetIdentifiers = expectedTargetIdentifiers().filter { it.uri != "bsp-workspace-root" }
    val compileParams = CompileParams(expectedTargetIdentifiers)
    compileParams.originId = UUID.randomUUID().toString()

    val expectedCompilerResult = CompileResult(StatusCode.OK)
    val expectedDiagnostic =
      Diagnostic(
        Range(Position(4, 2), Position(4, 2)),
        "match may not be exhaustive.\nIt would fail on the following input: C(_)\n  aa match {\n  ^",
      )
    expectedDiagnostic.severity = DiagnosticSeverity.WARNING

    val tmpDir = System.getenv()["BIT_WORKSPACE_DIR"]
    val expectedDocumentId = TextDocumentIdentifier("file://$tmpDir/scala_targets/Example.scala")
    val expectedDiagnosticsParam =
      PublishDiagnosticsParams(
        expectedDocumentId,
        expectedTargetIdentifiers[0],
        arrayListOf(expectedDiagnostic),
        true,
      )
    expectedDiagnosticsParam.originId = compileParams.originId
    expectedCompilerResult.originId = compileParams.originId

    return BazelBspTestScenarioStep("compile results") {
      checkDiagnostics(compileParams, expectedCompilerResult, listOf(expectedDiagnosticsParam))
      // The second build call publishes no diagnostics. Diagnostics are supposed to be cached on the client side.
      checkDiagnostics(compileParams, expectedCompilerResult, listOf())
    }
  }
}
