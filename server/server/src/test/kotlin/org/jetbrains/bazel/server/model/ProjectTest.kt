package org.jetbrains.bazel.server.model

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.AllowManualTargetsSyncSpec
import org.jetbrains.bazel.workspacecontext.AndroidMinSdkSpec
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bazel.workspacecontext.EnableNativeAndroidRules
import org.jetbrains.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bazel.workspacecontext.ExperimentalAddTransitiveCompileTimeJars
import org.jetbrains.bazel.workspacecontext.IdeJavaHomeOverrideSpec
import org.jetbrains.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bazel.workspacecontext.ImportRunConfigurationsSpec
import org.jetbrains.bazel.workspacecontext.NoPruneTransitiveCompileTimeJarsPatternsSpec
import org.jetbrains.bazel.workspacecontext.ShardSyncSpec
import org.jetbrains.bazel.workspacecontext.ShardingApproachSpec
import org.jetbrains.bazel.workspacecontext.SyncFlagsSpec
import org.jetbrains.bazel.workspacecontext.TargetShardSizeSpec
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.TransitiveCompileTimeJarsTargetKindsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("Project tests")
class ProjectTest {
  @Nested
  @DisplayName("project1 + project tests")
  inner class ProjectPlus {
    @Test
    fun `should throw an exception if workspaceRoot differs`() {
      // given
      val project1 =
        AspectSyncProject(
          workspaceRoot = Path("/path/to/workspace"),
          modules = emptyList(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = BazelRelease(0),
          workspaceContext = createMockWorkspaceContext(),
          workspaceName = "_main",
          targets = emptyMap(),
        )

      val project2 =
        AspectSyncProject(
          workspaceRoot = Path("/path/to/another/workspace"),
          modules = emptyList(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = BazelRelease(0),
          workspaceContext = createMockWorkspaceContext(),
          workspaceName = "_main",
          targets = emptyMap(),
        )

      // then
      shouldThrowWithMessage<IllegalStateException>(
        "Cannot add projects with different workspace roots: /path/to/workspace and /path/to/another/workspace",
      ) {
        project1 + project2
      }
    }

    @Test
    fun `should throw an exception if bazelRelease differs`() {
      // given
      val project1 =
        AspectSyncProject(
          workspaceRoot = Path("/path/to/workspace"),
          modules = emptyList(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = BazelRelease(21),
          workspaceContext = createMockWorkspaceContext(),
          workspaceName = "_main",
          targets = emptyMap(),
        )

      val project2 =
        AspectSyncProject(
          workspaceRoot = Path("/path/to/workspace"),
          modules = emptyList(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = BazelRelease(37),
          workspaceContext = createMockWorkspaceContext(),
          workspaceName = "_main",
          targets = emptyMap(),
        )

      // then
      shouldThrowWithMessage<IllegalStateException>(
        "Cannot add projects with different bazel versions: BazelRelease(major=21) and BazelRelease(major=37)",
      ) {
        project1 + project2
      }
    }

    @Test
    fun `should add two projects`() {
      // given
      val project1 =
        AspectSyncProject(
          workspaceRoot = Path("/path/to/workspace"),
          modules = listOf("//project1:module1".toMockModule(), "//project1:module2".toMockModule(), "//module".toMockModule()),
          libraries =
            mapOf(
              "@library1//lib".toLabel() to "@library1//lib".toMockLibrary(),
              "@library2//lib".toLabel() to "@library2//lib".toMockLibrary(),
            ),
          goLibraries =
            mapOf(
              "@golibrary1//lib".toLabel() to "@golibrary1//lib".toMockGoLibrary(),
              "@golibrary2//lib".toLabel() to "@golibrary2//lib".toMockGoLibrary(),
            ),
          invalidTargets = listOf("//invalid1".toLabel()),
          nonModuleTargets = listOf("//nonmodule1".toMockNonModuleTarget(), "//nonmodule2".toMockNonModuleTarget()),
          bazelRelease = BazelRelease(1),
          workspaceContext = createMockWorkspaceContext(targetsPattern = "//..."),
          workspaceName = "_main",
          targets = emptyMap(),
        )

      val project2 =
        AspectSyncProject(
          workspaceRoot = Path("/path/to/workspace"),
          modules =
            listOf(
              "//project2:module1".toMockModule(),
              "//project2:module2".toMockModule(),
              "//project2:module3".toMockModule(),
              "//module".toMockModule(),
            ),
          libraries =
            mapOf(
              "@library3//lib".toLabel() to "@library3//lib".toMockLibrary(),
              "@library4//lib".toLabel() to "@library4//lib".toMockLibrary(),
            ),
          goLibraries =
            mapOf(
              "@golibrary3//lib".toLabel() to "@golibrary3//lib".toMockGoLibrary(),
              "@golibrary4//lib".toLabel() to "@golibrary4//lib".toMockGoLibrary(),
            ),
          invalidTargets = listOf("//invalid2".toLabel()),
          nonModuleTargets = listOf("//nonmodule3".toMockNonModuleTarget()),
          bazelRelease = BazelRelease(1),
          workspaceContext = createMockWorkspaceContext(targetsPattern = "//other/..."),
          workspaceName = "_main",
          targets = emptyMap(),
        )

      // then
      val expectedNewProject =
        AspectSyncProject(
          workspaceRoot = Path("/path/to/workspace"),
          modules =
            listOf(
              "//project1:module1".toMockModule(),
              "//project1:module2".toMockModule(),
              "//project2:module1".toMockModule(),
              "//project2:module2".toMockModule(),
              "//project2:module3".toMockModule(),
              "//module".toMockModule(),
            ),
          libraries =
            mapOf(
              "@library1//lib".toLabel() to "@library1//lib".toMockLibrary(),
              "@library2//lib".toLabel() to "@library2//lib".toMockLibrary(),
              "@library3//lib".toLabel() to "@library3//lib".toMockLibrary(),
              "@library4//lib".toLabel() to "@library4//lib".toMockLibrary(),
            ),
          goLibraries =
            mapOf(
              "@golibrary1//lib".toLabel() to "@golibrary1//lib".toMockGoLibrary(),
              "@golibrary2//lib".toLabel() to "@golibrary2//lib".toMockGoLibrary(),
              "@golibrary3//lib".toLabel() to "@golibrary3//lib".toMockGoLibrary(),
              "@golibrary4//lib".toLabel() to "@golibrary4//lib".toMockGoLibrary(),
            ),
          invalidTargets = listOf("//invalid1".toLabel(), "//invalid2".toLabel()),
          nonModuleTargets =
            listOf(
              "//nonmodule3".toMockNonModuleTarget(),
              "//nonmodule1".toMockNonModuleTarget(),
              "//nonmodule2".toMockNonModuleTarget(),
            ),
          bazelRelease = BazelRelease(1),
          workspaceContext = createMockWorkspaceContext(targetsPattern = "//other/..."),
          workspaceName = "_main",
          targets = emptyMap(),
        )
      val newProject = project1 + project2

      newProject.workspaceRoot shouldBe expectedNewProject.workspaceRoot
      newProject.modules shouldContainExactlyInAnyOrder expectedNewProject.modules
      newProject.libraries shouldBe expectedNewProject.libraries
      newProject.goLibraries shouldBe expectedNewProject.goLibraries
      newProject.invalidTargets shouldContainExactlyInAnyOrder expectedNewProject.invalidTargets
      newProject.nonModuleTargets shouldContainExactlyInAnyOrder expectedNewProject.nonModuleTargets
      newProject.bazelRelease shouldBe expectedNewProject.bazelRelease
    }
  }

  private fun String.toMockModule(): Module =
    Module(
      label = this.toLabel(),
      isSynthetic = false,
      directDependencies = emptyList(),
      languages = emptySet(),
      tags = emptySet(),
      baseDirectory = Path("/path/to/$this"),
      sources = emptyList(),
      resources = emptySet(),
      sourceDependencies = emptySet(),
      languageData = null,
      environmentVariables = emptyMap(),
    )

  private fun String.toMockLibrary(): Library =
    Library(
      label = this.toLabel(),
      outputs = emptySet(),
      sources = emptySet(),
      dependencies = emptyList(),
    )

  private fun String.toMockGoLibrary(): GoLibrary = GoLibrary(label = toLabel())

  private fun String.toMockNonModuleTarget(): NonModuleTarget =
    NonModuleTarget(
      label = this.toLabel(),
      tags = emptySet(),
      baseDirectory = Path("/path/to/$this"),
    )

  private fun String.toLabel(): Label = Label.parse(this)

  private fun createMockWorkspaceContext(targetsPattern: String = "//..."): WorkspaceContext =
    WorkspaceContext(
      targets = TargetsSpec(listOf(Label.parse(targetsPattern)), emptyList()),
      directories = DirectoriesSpec(listOf(Path(".")), emptyList()),
      buildFlags = BuildFlagsSpec(emptyList()),
      syncFlags = SyncFlagsSpec(emptyList()),
      bazelBinary = BazelBinarySpec(Path("bazel")),
      allowManualTargetsSync = AllowManualTargetsSyncSpec(true),
      dotBazelBspDirPath = DotBazelBspDirPathSpec(Path(".bazelbsp")),
      importDepth = ImportDepthSpec(-1),
      enabledRules = EnabledRulesSpec(emptyList()),
      ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(Path("java_home")),
      experimentalAddTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJars(false),
      experimentalTransitiveCompileTimeJarsTargetKinds = TransitiveCompileTimeJarsTargetKindsSpec(emptyList()),
      experimentalNoPruneTransitiveCompileTimeJarsPatterns = NoPruneTransitiveCompileTimeJarsPatternsSpec(emptyList()),
      enableNativeAndroidRules = EnableNativeAndroidRules(false),
      androidMinSdkSpec = AndroidMinSdkSpec(null),
      shardSync = ShardSyncSpec(false),
      targetShardSize = TargetShardSizeSpec(1000),
      shardingApproachSpec = ShardingApproachSpec(null),
      importRunConfigurations = ImportRunConfigurationsSpec(emptyList()),
    )
}
