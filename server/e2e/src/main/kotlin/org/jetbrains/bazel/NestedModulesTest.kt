package org.jetbrains.bazel

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.time.Duration.Companion.seconds

object NestedModulesTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("@//...", "@inner//..."),
          ),
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
      // compareWorkspaceRepoMappingResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    error("not needed")
  }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) {
      testClient.test(60.seconds) { session ->
        val targetsResult =
          session.server.workspaceBuildTargets(
            WorkspaceBuildTargetParams(
              WorkspaceBuildTargetSelector.AllTargets,
            ),
          )
        targetsResult.targets.size shouldBe 4
        targetsResult.targets.map { it.key } shouldContainExactlyInAnyOrder
          listOf(
            Label.parse("@@inner+//:lib_inner"),
            Label.parse("@@inner+//:bin_inner"),
            Label.parse("@//:lib_outer"),
            Label.parse("@//:bin_outer"),
          )
      }

      //  targetsResult.targets
      //    .flatMap { it.sources }
      //    .map { it.path.relativeTo(Path(workspaceDir)).toString() } shouldContainExactlyInAnyOrder
      //    listOf(
      //      "BinOuter.java",
      //      "LibOuter.java",
      //      "inner/BinInner.java",
      //      "inner/LibInner.java",
      //    )
      //
      //  targetsResult.targets
      //    .map { it.baseDirectory }
      //    .map { it.relativeTo(Path(workspaceDir)).toString() } shouldContainExactlyInAnyOrder
      //    listOf("inner", "inner", "", "")
      // }
    }

  // private fun compareWorkspaceRepoMappingResults(): BazelBspTestScenarioStep =
  //  BazelBspTestScenarioStep(
  //    "compare workspace repo mapping results",
  //  ) {
  //    testClient.test(60.seconds) { session ->
  //      val repoMapping = session.server.workspaceBazelRepoMapping()
  //
  //      repoMapping.repoMapping.apparentRepoNameToCanonicalName shouldBe
  //        mapOf(
  //          "" to "",
  //          "bazelbsp_aspect" to "+_repo_rules+bazelbsp_aspect",
  //          "local_config_platform" to "local_config_platform",
  //          "rules_java" to "rules_java+",
  //          "bazel_tools" to "bazel_tools",
  //          "outer" to "",
  //          "inner" to "inner+",
  //        )
  //
  //      val canonicalMapping = repoMapping.canonicalRepoNameToPath
  //      canonicalMapping.keys shouldBe repoMapping.apparentRepoNameToCanonicalName.values.toSet()
  //      canonicalMapping[""] shouldBe Path("$workspaceDir/")
  //      canonicalMapping
  //        .getValue(
  //          "+_repo_rules+bazelbsp_aspect",
  //        ).toString()
  //        .shouldEndWith("/external/+_repo_rules+bazelbsp_aspect")
  //      canonicalMapping.getValue("local_config_platform").toString().shouldEndWith("/external/local_config_platform")
  //      canonicalMapping.getValue("rules_java+").toString().shouldEndWith("/external/rules_java+")
  //      canonicalMapping["inner+"] shouldBe Path("$workspaceDir/inner/")
  //      canonicalMapping["bazel_tools"].toString() shouldEndWith ("/external/bazel_tools")
  //
  //      for (path in canonicalMapping.values) {
  //        path.shouldExist()
  //      }
  //    }
  //  }
}
