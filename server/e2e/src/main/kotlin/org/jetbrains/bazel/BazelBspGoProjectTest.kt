package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

object BazelBspGoProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()
  private val defaultSdkHomePath = Path("\$BAZEL_OUTPUT_BASE_PATH/external/go_sdk/")
  private val enabledRules: List<String>
    get() = listOf("io_bazel_rules_go")
  private val gazelleTargetToCall = "//:gazelle"

  override fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("//..."),
            enabledRules = enabledRules,
            gazelleTarget = gazelleTargetToCall,
          ),
      ),
    )
  }

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult =
    WorkspaceBuildTargetsResult(
      targets = mapOf(),
      rootTargets = setOf(),
    )

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      workspaceBuildTargets(),
    )

  private fun workspaceBuildTargets(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep("workspace build targets") {
      testClient.testWorkspaceTargets(
        1.minutes,
        expectedWorkspaceBuildTargetsResult(),
      )
    }

  private fun exampleBuildTarget(): RawBuildTarget =
    createGoBuildTarget(
      targetDirectory = "example",
      targetName = "hello",
      tags = listOf(),
      kind =
        TargetKind(
          kindString = "go_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.GO),
        ),
      dependencies =
        listOf(
          Label.parse("$targetPrefix//example:go_default_library"),
        ),
      importPath = "",
    )

  private fun exampleLibBuildTarget(): RawBuildTarget =
    createGoBuildTarget(
      targetDirectory = "example",
      targetName = "go_default_library",
      tags = listOf(),
      kind =
        TargetKind(
          kindString = "go_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.GO),
        ),
      sources =
        listOf(
          SourceItem(
            Path("\$WORKSPACE/example/hello.go"),
            false,
          ),
        ),
      generatedSources = listOf(Path("\$WORKSPACE/example/hello.go")),
      importPath = "go-project/example",
      dependencies =
        listOf(
          Label.parse("$targetPrefix//lib:go_default_library"),
        ),
    )

  private fun libBuildTarget(): RawBuildTarget =
    createGoBuildTarget(
      targetDirectory = "lib",
      targetName = "go_default_library",
      tags = listOf(),
      kind =
        TargetKind(
          kindString = "go_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.GO),
        ),
      importPath = "go-project/lib",
      sources =
        listOf(
          SourceItem(
            Path("\$WORKSPACE/lib/example_lib.go"),
            false,
          ),
        ),
      generatedSources = listOf(Path("\$WORKSPACE/lib/example_lib.go")),
    )

  private fun libTestBuildTarget(): RawBuildTarget =
    createGoBuildTarget(
      targetDirectory = "lib",
      targetName = "go_default_test",
      tags = listOf(),
      kind =
        TargetKind(
          kindString = "go_test",
          ruleType = RuleType.TEST,
          languageClasses = setOf(LanguageClass.GO),
        ),
      importPath = "",
      sources =
        listOf(
          SourceItem(
            Path("\$WORKSPACE/lib/example_test.go"),
            false,
          ),
        ),
      dependencies = listOf(Label.parse("$targetPrefix//lib:go_default_library")),
      generatedSources = listOf(Path("\$WORKSPACE/lib/example_test.go")),
      libraryLabels = listOf(Label.parse("$targetPrefix//lib:go_default_library")),
    )

  private fun createGoBuildTarget(
    targetDirectory: String,
    targetName: String,
    tags: List<String>,
    kind: TargetKind,
    importPath: String,
    sdkHomePath: Path = defaultSdkHomePath,
    dependencies: List<Label> = listOf(),
    sources: List<SourceItem> = emptyList(),
    generatedSources: List<Path> = emptyList(),
    libraryLabels: List<Label> = emptyList(),
  ): RawBuildTarget {
    val goBuildTarget =
      GoBuildTarget(
        sdkHomePath = sdkHomePath,
        importPath = importPath,
        generatedLibraries = emptyList(),
        generatedSources = generatedSources,
        libraryLabels = libraryLabels,
      )

    val buildTargetData =
      RawBuildTarget(
        Label.parse("$targetPrefix//$targetDirectory:$targetName"),
        tags = tags,
        dependencies = dependencies,
        kind = kind,
        baseDirectory = Path("\$WORKSPACE/$targetDirectory/"),
        data = goBuildTarget,
        sources = sources,
        resources = emptyList(),
      )
    return buildTargetData
  }

  val gazelleTarget =
    RawBuildTarget(
      id = Label.parse("//:gazelle-runner"),
      tags = emptyList(),
      dependencies = emptyList(),
      kind = TargetKind(kindString = "_gazelle_runner", ruleType = RuleType.BINARY),
      baseDirectory = Path("\$WORKSPACE/"),
      sources = emptyList(),
      resources = emptyList(),
    )
}
