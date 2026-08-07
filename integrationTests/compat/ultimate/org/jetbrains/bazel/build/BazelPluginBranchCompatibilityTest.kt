// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.build

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.intellij.build.BuildOptions
import org.jetbrains.intellij.build.BuildPaths.Companion.ULTIMATE_HOME
import org.jetbrains.intellij.build.VerifierIdeInfo
import org.jetbrains.intellij.build.createPluginVerifier
import org.jetbrains.intellij.build.dependencies.TeamCityHelper
import org.jetbrains.intellij.build.dev.BuildRequest
import org.jetbrains.intellij.build.dev.buildProductInProcess
import org.jetbrains.intellij.build.impl.SnapshotBuildNumber
import org.jetbrains.intellij.build.telemetry.block
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

private val KNOWN_ERRORS_FILE = ULTIMATE_HOME.resolve("plugins/bazel/integrationTests/compat/testData/known_errors_branch.txt")

/**
 * Tests the compatibility of the Bazel plugin built from branch sources against IDEA Ultimate dev-built from the very same revision.
 *
 * Unlike [BazelPluginCompatibilityTest], which verifies the plugin against a released IDE snapshot pinned by `versions.bzl`
 * `SINCE_VERSION`, this test compares the branch against itself: both the plugin and the IDE come from the current working tree.
 * A failure therefore means the plugin cannot load into the platform it was compiled against - packaging or API drift inside
 * the branch - rather than incompatibility with an older or newer release.
 *
 * The plugin's `since-build`/`until-build` range is pinned to [SnapshotBuildNumber.BASE] so that the verifier accepts the
 * dev-built IDE regardless of what `versions.bzl` declares.
 *
 * Driven by the `//plugins/bazel/integrationTests:branch_compatibility_test` Bazel target, which sets
 * `-Dbazel.plugin.branch.compatibility.enabled=true`. Without that property the class is skipped, so the unfiltered
 * `:integrationTests_test` target does not pay for a full IDE dev build.
 *
 * Directories used by this test:
 * - `out/bazel-plugin-branch` - Bazel plugin build output
 * - `out/bazel-plugin-home` - Plugin verifier home directory
 * - `out/bazel-plugin-reports` - Compatibility reports
 */
@EnabledIfSystemProperty(named = "bazel.plugin.branch.compatibility.enabled", matches = "true")
class BazelPluginBranchCompatibilityTest {
  companion object {
    private const val PLATFORM_PREFIX = "idea"
    private const val PRODUCT_CODE = "IU"
  }

  @Test
  fun `Bazel plugin branch compatibility test`() {
    runBlocking(Dispatchers.Default) {
      val loggedErrors = mutableListOf<String>()

      val bazelPlugin = block("Building Bazel plugin") {
        buildBazelPluginZip(
          outRootDir = ULTIMATE_HOME.resolve("out/bazel-plugin-branch"),
          versions = BazelVersions(
            pluginVersion = parseBzlVersions().pluginVersion,
            sinceVersion = SnapshotBuildNumber.BASE,
            untilVersion = "${SnapshotBuildNumber.BASE}.*",
          ),
        ) {
          disableEmbeddedFrontend()
          skipBuildSteps(BuildOptions.SEARCHABLE_OPTIONS_INDEX_STEP, BuildOptions.KEYMAP_PLUGINS_STEP)
        }
      }

      val idePath = block("Building IDEA Ultimate from sources") {
        val outputRoot = ULTIMATE_HOME.resolve("out").run { if (TeamCityHelper.isUnderTeamCity) resolve("tests") else this }
        buildProductInProcess(BuildRequest(
          platformPrefix = PLATFORM_PREFIX,
          additionalModules = emptyList(),
          projectDir = ULTIMATE_HOME,
          keepHttpClient = false,
          isBootClassPathCorrect = false,
          classesOutputDirectory = outputRoot.resolve("classes"),
        ))
      }

      val verifier = block("Downloading plugin verifier") {
        createPluginVerifier(errorHandler = { loggedErrors.add(it.trim()) })
      }

      val hasErrors = verifyBazelPluginCompatibility(
        verifier = verifier,
        bazelPlugin = bazelPlugin,
        ide = VerifierIdeInfo(
          installationPath = idePath,
          productCode = PRODUCT_CODE,
          productBuild = SnapshotBuildNumber.VALUE,
        ),
      )

      if (hasErrors) {
        assertNoNewErrors(
          loggedErrors = loggedErrors,
          knownErrorsFile = KNOWN_ERRORS_FILE,
          failureContext = """
            The Bazel plugin built from branch sources is incompatible with IDEA Ultimate ${SnapshotBuildNumber.VALUE} built from the same revision.
            Plugin and IDE come from the same working tree, so this is packaging or API drift inside the branch itself:
            the plugin cannot be loaded by the platform it was compiled against.
          """.trimIndent(),
        )
      }
    }
  }
}
