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
import org.jetbrains.bsp.protocol.GoLibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

object BazelBspGoProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()
  private val bazelTestClient = createTestkitClient()
  private val defaultSdkHomePath = Path("\$BAZEL_OUTPUT_BASE_PATH/external/go_sdk/")
  private val enabledRules: List<String>
    get() = listOf("io_bazel_rules_go")

  override fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("//..."),
            enabledRules = enabledRules,
          ),
      ),
    )
  }

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult =
    WorkspaceBuildTargetsResult(
      listOf(
        exampleBuildTarget(),
        libBuildTarget(),
        libTestBuildTarget(),
      ),
    )

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      workspaceBuildTargets(),
      librariesResult(),
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
          Label.parse("$targetPrefix//lib:go_default_library"),
          Label.parse("@org_golang_x_text//cases:cases"),
        ),
      importPath = "example/hello",
      sources =
        listOf(
          SourceItem(
            Path("\$WORKSPACE/example/hello.go"),
            false,
          ),
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
      importPath = "example.com/lib",
      sources =
        listOf(
          SourceItem(
            Path("\$WORKSPACE/lib/example_lib.go"),
            false,
          ),
        ),
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
      importPath = "testmain",
      sources =
        listOf(
          SourceItem(
            Path("\$WORKSPACE/lib/example_test.go"),
            false,
          ),
        ),
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
  ): RawBuildTarget {
    val goBuildTarget =
      GoBuildTarget(
        sdkHomePath = sdkHomePath,
        importPath = importPath,
        generatedLibraries = emptyList(),
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

  private fun librariesResult(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep("libraries results") {
      bazelTestClient.test(timeout = 1.minutes) { session ->
        val libraries = session.server.workspaceGoLibraries()
        bazelTestClient.assertJsonEquals<WorkspaceGoLibrariesResult>(expectedLibrariesResult(), libraries)
      }
    }

  private fun expectedLibrariesResult(): WorkspaceGoLibrariesResult {
    val libraryRoot = "\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/external/org_golang_x_text/"
    val expectedLibraries =
      listOf(
        GoLibraryItem(
          id = Label.parse("@org_golang_x_text//cases:cases"),
          goImportPath = "golang.org/x/text/cases",
          goRoot = Path(libraryRoot + "cases/"),
        ),
        GoLibraryItem(
          id = Label.parse("@org_golang_x_text//internal:internal"),
          goImportPath = "golang.org/x/text/internal",
          goRoot = Path(libraryRoot + "internal/"),
        ),
        GoLibraryItem(
          id = Label.parse("@org_golang_x_text//internal/language:language"),
          goImportPath = "golang.org/x/text/internal/language",
          goRoot = Path(libraryRoot + "internal/language/"),
        ),
        GoLibraryItem(
          id = Label.parse("@org_golang_x_text//internal/language/compact:compact"),
          goImportPath = "golang.org/x/text/internal/language/compact",
          goRoot = Path(libraryRoot + "internal/language/compact/"),
        ),
        GoLibraryItem(
          id = Label.parse("@org_golang_x_text//internal/tag:tag"),
          goImportPath = "golang.org/x/text/internal/tag",
          goRoot = Path(libraryRoot + "internal/tag/"),
        ),
        GoLibraryItem(
          id = Label.parse("@org_golang_x_text//language:language"),
          goImportPath = "golang.org/x/text/language",
          goRoot = Path(libraryRoot + "language/"),
        ),
        GoLibraryItem(
          id = Label.parse("@org_golang_x_text//transform:transform"),
          goImportPath = "golang.org/x/text/transform",
          goRoot = Path(libraryRoot + "transform/"),
        ),
        GoLibraryItem(
          id = Label.parse("@org_golang_x_text//unicode/norm:norm"),
          goImportPath = "golang.org/x/text/unicode/norm",
          goRoot = Path(libraryRoot + "unicode/norm/"),
        ),
      )
    return WorkspaceGoLibrariesResult(expectedLibraries)
  }
}
