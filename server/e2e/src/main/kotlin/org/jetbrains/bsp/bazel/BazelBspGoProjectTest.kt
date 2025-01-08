package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.GoLibraryItem
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import java.net.URI
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object BazelBspGoProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()
  private val bazelTestClient = createBazelClient()
  private val defaultSdkHomePath = URI("file://\$BAZEL_OUTPUT_BASE_PATH/external/go_sdk/")
  private val enabledRules: List<String>
    get() = listOf("io_bazel_rules_go")

  override fun additionalServerInstallArguments(): Array<String> =
    arrayOf(
      "--enabled-rules",
      *enabledRules.toTypedArray(),
    )

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
      sourcesResults(),
      librariesResult(),
    )

  private fun workspaceBuildTargets(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep("workspace build targets") {
      testClient.testWorkspaceTargets(
        1.minutes,
        expectedWorkspaceBuildTargetsResult(),
      )
    }

  private fun sourcesResults(): BazelBspTestScenarioStep {
    val targetHello =
      SourceItem(
        "file://\$WORKSPACE/example/hello.go",
        SourceItemKind.FILE,
        false,
      )
    val targetHelloSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//example:hello"),
        listOf(targetHello),
      )
    targetHelloSources.roots = listOf()

    val targetGoDefaultLibrary =
      SourceItem(
        "file://\$WORKSPACE/lib/example_lib.go",
        SourceItemKind.FILE,
        false,
      )
    val targetGoDefaultLibrarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//lib:go_default_library"),
        listOf(targetGoDefaultLibrary),
      )
    targetGoDefaultLibrarySources.roots = listOf()

    val targetGoDefaultTest =
      SourceItem(
        "file://\$WORKSPACE/lib/example_test.go",
        SourceItemKind.FILE,
        false,
      )
    val targetGoDefaultTestSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//lib:go_default_test"),
        listOf(
          targetGoDefaultTest,
        ),
      )
    targetGoDefaultTestSources.roots = listOf()

    val sourcesParams = SourcesParams(expectedTargetIdentifiers())
    val expectedSourcesResult =
      SourcesResult(
        listOf(
          targetHelloSources,
          targetGoDefaultLibrarySources,
          targetGoDefaultTestSources,
        ),
      )
    return BazelBspTestScenarioStep("sources results") {
      testClient.testSources(30.seconds, sourcesParams, expectedSourcesResult)
    }
  }

  private fun exampleBuildTarget(): BuildTarget =
    createGoBuildTarget(
      targetDirectory = "example",
      targetName = "hello",
      tags = listOf("application"),
      capabilities =
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      dependencies =
        listOf(
          BuildTargetIdentifier("$targetPrefix//lib:go_default_library"),
          BuildTargetIdentifier("@org_golang_x_text//cases:cases"),
        ),
      importPath = "example/hello",
    )

  private fun libBuildTarget(): BuildTarget =
    createGoBuildTarget(
      targetDirectory = "lib",
      targetName = "go_default_library",
      tags = listOf("library"),
      capabilities =
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      importPath = "example.com/lib",
    )

  private fun libTestBuildTarget(): BuildTarget =
    createGoBuildTarget(
      targetDirectory = "lib",
      targetName = "go_default_test",
      tags = listOf("test"),
      capabilities =
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = true
          it.canRun = false
          it.canDebug = false
        },
      importPath = "testmain",
    )

  private fun createGoBuildTarget(
    targetDirectory: String,
    targetName: String,
    tags: List<String>,
    capabilities: BuildTargetCapabilities,
    importPath: String,
    sdkHomePath: URI = defaultSdkHomePath,
    dependencies: List<BuildTargetIdentifier> = listOf(),
  ): BuildTarget {
    val goBuildTarget =
      GoBuildTarget(
        sdkHomePath = sdkHomePath,
        importPath = importPath,
        generatedLibraries = emptyList(),
      )

    val buildTargetData =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//$targetDirectory:$targetName"),
        tags,
        listOf("go"),
        dependencies,
        capabilities,
      )

    buildTargetData.displayName = "$targetPrefix//$targetDirectory:$targetName"
    buildTargetData.baseDirectory = "file://\$WORKSPACE/$targetDirectory/"
    buildTargetData.data = goBuildTarget
    buildTargetData.dataKind = "go"

    return buildTargetData
  }

  private fun librariesResult(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep("libraries results") {
      bazelTestClient.test(timeout = 1.minutes) { session, _ ->
        val libraries = session.server.workspaceGoLibraries().await()
        bazelTestClient.assertJsonEquals<WorkspaceGoLibrariesResult>(expectedLibrariesResult(), libraries)
      }
    }

  private fun expectedLibrariesResult(): WorkspaceGoLibrariesResult {
    val libraryRoot = "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/external/org_golang_x_text/"
    val expectedLibraries =
      listOf(
        GoLibraryItem(
          id = BuildTargetIdentifier("@org_golang_x_text//cases:cases"),
          goImportPath = "golang.org/x/text/cases",
          goRoot = URI.create(libraryRoot + "cases/"),
        ),
        GoLibraryItem(
          id = BuildTargetIdentifier("@org_golang_x_text//internal:internal"),
          goImportPath = "golang.org/x/text/internal",
          goRoot = URI.create(libraryRoot + "internal/"),
        ),
        GoLibraryItem(
          id = BuildTargetIdentifier("@org_golang_x_text//internal/language:language"),
          goImportPath = "golang.org/x/text/internal/language",
          goRoot = URI.create(libraryRoot + "internal/language/"),
        ),
        GoLibraryItem(
          id = BuildTargetIdentifier("@org_golang_x_text//internal/language/compact:compact"),
          goImportPath = "golang.org/x/text/internal/language/compact",
          goRoot = URI.create(libraryRoot + "internal/language/compact/"),
        ),
        GoLibraryItem(
          id = BuildTargetIdentifier("@org_golang_x_text//internal/tag:tag"),
          goImportPath = "golang.org/x/text/internal/tag",
          goRoot = URI.create(libraryRoot + "internal/tag/"),
        ),
        GoLibraryItem(
          id = BuildTargetIdentifier("@org_golang_x_text//language:language"),
          goImportPath = "golang.org/x/text/language",
          goRoot = URI.create(libraryRoot + "language/"),
        ),
        GoLibraryItem(
          id = BuildTargetIdentifier("@org_golang_x_text//transform:transform"),
          goImportPath = "golang.org/x/text/transform",
          goRoot = URI.create(libraryRoot + "transform/"),
        ),
        GoLibraryItem(
          id = BuildTargetIdentifier("@org_golang_x_text//unicode/norm:norm"),
          goImportPath = "golang.org/x/text/unicode/norm",
          goRoot = URI.create(libraryRoot + "unicode/norm/"),
        ),
      )
    return WorkspaceGoLibrariesResult(expectedLibraries)
  }
}
