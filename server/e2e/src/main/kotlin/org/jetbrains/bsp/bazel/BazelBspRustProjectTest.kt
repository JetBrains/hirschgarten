package org.jetbrains.bsp.bazel

import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.RustCrateType
import org.jetbrains.bsp.protocol.RustDepKindInfo
import org.jetbrains.bsp.protocol.RustDependency
import org.jetbrains.bsp.protocol.RustPackage
import org.jetbrains.bsp.protocol.RustRawDependency
import org.jetbrains.bsp.protocol.RustTarget
import org.jetbrains.bsp.protocol.RustTargetKind
import org.jetbrains.bsp.protocol.RustWorkspaceParams
import org.jetbrains.bsp.protocol.RustWorkspaceResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object BazelBspRustProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      workspaceBuildTargets(),
      rustWorkspaceResults(),
    )

  private fun workspaceBuildTargets(): BazelBspTestScenarioStep {
    val workspaceBuildTargetsResult = expectedWorkspaceBuildTargetsResult()

    return BazelBspTestScenarioStep("workspace build targets") {
      testClient.testWorkspaceTargets(
        1.minutes,
        workspaceBuildTargetsResult,
      )
    }
  }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult =
    WorkspaceBuildTargetsResult(
      listOf(
        makeExampleLib(),
        makeExampleFeature(),
        makeExample(),
      ),
    )

  private fun makeExampleLib(): BuildTarget {
    val exampleLibDependencies =
      listOf(
        BuildTargetIdentifier("@crate_index__instant-0.1.12//:instant"),
      )
    return makeBuildTarget("example-lib", "example_lib", "library", exampleLibDependencies, false)
  }

  private fun makeExample(): BuildTarget {
    val exampleDependencies =
      listOf(
        BuildTargetIdentifier("$targetPrefix//example-lib:example_lib"),
        BuildTargetIdentifier("@crate_index__itertools-0.10.5//:itertools"),
      )
    return makeBuildTarget("example", "example", "application", exampleDependencies, true)
  }

  private fun makeExampleFeature(): BuildTarget {
    val exampleFeatureDependencies =
      listOf(
        BuildTargetIdentifier("$targetPrefix//example-lib:example_lib"),
        BuildTargetIdentifier("@crate_index__itertools-0.10.5//:itertools"),
        BuildTargetIdentifier("@crate_index__itoa-1.0.6//:itoa"),
      )
    return makeBuildTarget("example", "example_foo", "application", exampleFeatureDependencies, true)
  }

  private fun makeBuildTarget(
    packageName: String,
    name: String,
    type: String,
    dependencies: List<BuildTargetIdentifier>,
    canRun: Boolean,
  ): BuildTarget {
    val buildtarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//$packageName:$name"),
        listOf(type),
        listOf("rust"),
        dependencies,
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = canRun,
          canDebug = false,
        ),
        displayName = "$targetPrefix//$packageName:$name",
        baseDirectory = "file://\$WORKSPACE/$packageName/",
      )

    return buildtarget
  }

  private fun rustWorkspaceResults(): BazelBspTestScenarioStep {
    val expectedTargetIdentifiers =
      expectedTargetIdentifiers().filter {
        it.uri != "bsp-workspace-root"
      }
    val expectedResolvedTargets =
      expectedTargetIdentifiers.filter {
        it.uri != "@//example"
      }
    val expectedRustWorkspaceResult =
      RustWorkspaceResult(
        expectedPackages(),
        expectedRawDependencies(),
        expectedDependencies(),
        expectedResolvedTargets,
      )
    val rustWorkspaceParams = RustWorkspaceParams(expectedTargetIdentifiers)

    return BazelBspTestScenarioStep(
      "rustWorkspace results",
    ) {
      testClient.testRustWorkspace(
        30.seconds,
        rustWorkspaceParams,
        expectedRustWorkspaceResult,
      )
    }
  }

  private fun expectedPackages(): List<RustPackage> {
    val exampleLibTargets =
      listOf(
        RustTarget(
          "example_lib",
          "file://\$WORKSPACE/example-lib/lib.rs",
          RustTargetKind.LIB,
          "2018",
          false,
          crateTypes = listOf(RustCrateType.RLIB),
          requiredFeatures = setOf(),
        ),
      )

    val exampleTargets =
      listOf(
        expectedExampleTarget("example"),
        expectedExampleTarget("example_foo", setOf("foo")),
      )

    return listOf(
      expectedPackageFromDependency("cfg-if", "1.0.0"),
      expectedPackageFromDependency("either", "1.8.1", setOf("use_std")),
      expectedPackageFromDependency("instant", "0.1.12"),
      expectedPackageFromDependency("itertools", "0.10.5", setOf("default", "use_alloc", "use_std")),
      expectedPackageFromDependency("itoa", "1.0.6"),
      expectedPackageFromWorkspace("example-lib", exampleLibTargets, exampleLibTargets),
      expectedPackageFromWorkspace("example", listOf(exampleTargets[1]), exampleTargets, setOf("foo")),
    )
  }

  private fun expectedExampleTarget(name: String, features: Set<String> = setOf()): RustTarget =
    RustTarget(
      name,
      "file://\$WORKSPACE/example/main.rs",
      RustTargetKind.BIN,
      "2018",
      false,
      crateTypes = listOf(),
      requiredFeatures = features,
    )

  private fun expectedPackageFromDependency(
    name: String,
    version: String,
    features: Set<String> = setOf(),
  ): RustPackage {
    val packageId = "crate_index__$name-$version"
    val packageName = "@$packageId//"
    val packageRootUrl = "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/rust_test/external/$packageId/"

    val targets = expectedTargetFromDependency(name, packageRootUrl, features)

    return RustPackage(
      packageName,
      packageRootUrl,
      packageName,
      version,
      "DEPENDENCY",
      "2018",
      targets,
      targets,
      features.associateWith { setOf() },
      features,
      env = expectedEnv(packageName, "$packageRootUrl$packageId//", version),
      source = "bazel+https://github.com/rust-lang/crates.io-index",
    )
  }

  private fun expectedTargetFromDependency(
    name: String,
    packageRootUrl: String,
    features: Set<String> = setOf(),
  ): List<RustTarget> =
    listOf(
      RustTarget(
        name.replace("-", "_"),
        packageRootUrl + "src/lib.rs",
        RustTargetKind.LIB,
        "2018",
        false,
        crateTypes = listOf(RustCrateType.RLIB),
        requiredFeatures = features,
      ),
    )

  private fun expectedEnv(
    packageName: String,
    manifestDir: String,
    version: String,
  ): Map<String, String> {
    val (major, minor, patch) = version.split(".")

    return mapOf(
      "CARGO" to "cargo",
      "CARGO_CRATE_NAME" to packageName,
      "CARGO_MANIFEST_DIR" to manifestDir,
      "CARGO_PKG_AUTHORS" to "",
      "CARGO_PKG_DESCRIPTION" to "",
      "CARGO_PKG_LICENSE" to "",
      "CARGO_PKG_LICENSE_FILE" to "",
      "CARGO_PKG_NAME" to packageName,
      "CARGO_PKG_REPOSITORY" to "",
      "CARGO_PKG_VERSION" to version,
      "CARGO_PKG_VERSION_MAJOR" to major,
      "CARGO_PKG_VERSION_MINOR" to minor,
      "CARGO_PKG_VERSION_PATCH" to patch,
      "CARGO_PKG_VERSION_PRE" to "",
    )
  }

  private fun expectedPackageFromWorkspace(
    name: String,
    resolvedTargets: List<RustTarget>,
    allTargets: List<RustTarget>,
    features: Set<String> = setOf(),
  ): RustPackage {
    val packageName = "@//$name"

    return RustPackage(
      packageName,
      "file://\$WORKSPACE/$name/",
      packageName,
      "0.0.0",
      "WORKSPACE",
      "2018",
      resolvedTargets,
      allTargets,
      features.associateWith { setOf() },
      features,
      env = expectedEnv(packageName, "file://\$WORKSPACE/$name///$name", "0.0.0"),
    )
  }

  private fun expectedDependencies(): Map<String, List<RustDependency>> {
    val exampleDependencies =
      createDependency(
        listOf(
          Pair("@//example-lib", "example_lib"),
          Pair("@crate_index__itertools-0.10.5//", "itertools"),
          Pair("@crate_index__itoa-1.0.6//", "itoa"),
        ),
      )
    val exampleLibDependencies = createDependency(listOf(Pair("@crate_index__instant-0.1.12//", "instant")))
    val instantDependencies = createDependency(listOf(Pair("@crate_index__cfg-if-1.0.0//", "cfg_if")))
    val itertoolsDependencies = createDependency(listOf(Pair("@crate_index__either-1.8.1//", "either")))

    return mapOf(
      "@//example" to exampleDependencies,
      "@//example-lib" to exampleLibDependencies,
      "@crate_index__instant-0.1.12//" to instantDependencies,
      "@crate_index__itertools-0.10.5//" to itertoolsDependencies,
    )
  }

  private fun createDependency(dependenciesNames: List<Pair<String, String>>): List<RustDependency> =
    dependenciesNames.map { dep ->
      RustDependency(
        dep.first,
        name = dep.second,
        depKinds = listOf(RustDepKindInfo("normal")),
      )
    }

  private fun expectedRawDependencies(): Map<String, List<RustRawDependency>> {
    val exampleDependencies =
      createRawDependency(listOf("@//example-lib:example_lib", "@crate_index__itertools-0.10.5//:itertools"))
    val exampleLibDependencies = createRawDependency(listOf("@crate_index__instant-0.1.12//:instant"))
    val instantDependencies = createRawDependency(listOf("@crate_index__cfg-if-1.0.0//:cfg_if"))
    val itertoolsDependencies = createRawDependency(listOf("@crate_index__either-1.8.1//:either"))

    return mapOf(
      "@//example" to exampleDependencies,
      "@//example-lib" to exampleLibDependencies,
      "@crate_index__instant-0.1.12//" to instantDependencies,
      "@crate_index__itertools-0.10.5//" to itertoolsDependencies,
    )
  }

  private fun createRawDependency(rawDependenciesNames: List<String>): List<RustRawDependency> =
    rawDependenciesNames.map { dep -> RustRawDependency(dep, false, true, setOf()) }
}
