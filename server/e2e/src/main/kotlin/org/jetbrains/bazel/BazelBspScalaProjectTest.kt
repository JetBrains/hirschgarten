package org.jetbrains.bazel

import org.apache.logging.log4j.LogManager
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.Diagnostic
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.Position
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.Range
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import java.util.UUID
import kotlin.io.path.Path
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

  override fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("//..."),
            enabledRules = listOf("io_bazel_rules_scala", "rules_java", "rules_jvm"),
          ),
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
      compileWithWarnings(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHome = Path("\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_$javaHomeArchitecture/")
    val jvmBuildTarget =
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = "11",
      )
    val scalaBuildTarget =
      ScalaBuildTarget(
        "2.12.14",
        listOf(
          Path("\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_compiler/scala-compiler-2.12.14.jar"),
          Path("\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar"),
          Path("\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar"),
        ),
        jvmBuildTarget = jvmBuildTarget,
        scalacOptions = listOf("-target:jvm-1.8"),
      )

    val target =
      RawBuildTarget(
        Label.parse("$targetPrefix//scala_targets:library"),
        listOf(),
        listOf(
          Label.synthetic("scala-compiler-2.12.14.jar"),
          Label.synthetic("scala-library-2.12.14.jar"),
          Label.synthetic("scala-reflect-2.12.14.jar"),
        ),
        TargetKind(
          kindString = "scala_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.SCALA, LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/scala_targets/"),
        data = scalaBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/scala_targets/Example.scala"),
              generated = false,
              jvmPackagePrefix = "example",
            ),
          ),
        resources = listOf(),
      )
    return WorkspaceBuildTargetsResult(
      targets = mapOf(),
      rootTargets = setOf(),
    )
  }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) { testClient.testWorkspaceTargets(120.seconds, expectedWorkspaceBuildTargetsResult()) }

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

    val expectedCompilerResult = CompileResult(BazelStatus.SUCCESS)
    val expectedDiagnostic =
      Diagnostic(
        Range(Position(4, 2), Position(4, 2)),
        "match may not be exhaustive.\nIt would fail on the following input: C(_)\n  aa match {\n  ^",
        severity = DiagnosticSeverity.WARNING,
      )

    val tmpDir = System.getenv()["BIT_WORKSPACE_DIR"]
    val expectedDocumentId = TextDocumentIdentifier(Path("$tmpDir/scala_targets/Example.scala"))
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
