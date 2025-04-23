package org.jetbrains.bazel.workspacecontext.provider

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewAllowManualTargetsSyncSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBazelBinarySection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBuildFlagsSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewEnabledRulesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewImportDepthSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewSyncFlagsSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewTargetsSection
import org.jetbrains.bazel.workspacecontext.AllowManualTargetsSyncSpec
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bazel.workspacecontext.SyncFlagsSpec
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class WorkspaceContextConstructorTest {
  @Nested
  @DisplayName("fun construct(projectView: ProjectView): WorkspaceContext tests")
  inner class ConstructTest {
    @Test
    fun `should return success if project view is valid`() {
      // given
      val workspaceRoot = Path("path/to/workspace")
      val dotBazelBspDirPath = Path("path/to/bazelbsp")
      val projectViewFilePath = workspaceRoot.resolve("path/to/project/view/file")
      val constructor = WorkspaceContextConstructor(workspaceRoot, dotBazelBspDirPath, projectViewFilePath)
      val projectView =
        ProjectView
          .Builder(
            targets =
              ProjectViewTargetsSection(
                listOf(
                  Label.parse("//included_target1"),
                  Label.parse("//included_target2"),
                  Label.parse("//included_target3"),
                ),
                listOf(Label.parse("//excluded_target1")),
              ),
            directories =
              ProjectViewDirectoriesSection(
                values =
                  listOf(
                    Path("path/to/included1"),
                    Path("path/to/included2"),
                  ),
                excludedValues =
                  listOf(
                    Path("path/to/excluded"),
                  ),
              ),
            buildFlags =
              ProjectViewBuildFlagsSection(
                listOf(
                  "--build_flag1=value1",
                  "--build_flag2=value2",
                  "--build_flag3=value3",
                ),
              ),
            syncFlags =
              ProjectViewSyncFlagsSection(
                listOf(
                  "--sync_flag1=value1",
                  "--sync_flag2=value2",
                  "--sync_flag3=value3",
                ),
              ),
            bazelBinary = ProjectViewBazelBinarySection(Path("/path/to/bazel")),
            allowManualTargetsSync = ProjectViewAllowManualTargetsSyncSection(false),
            importDepth = ProjectViewImportDepthSection(3),
            enabledRules = ProjectViewEnabledRulesSection(listOf("rules_scala")),
          ).build()

      // when
      val workspaceContext = constructor.construct(projectView)

      // then
      val expectedTargets =
        TargetsSpec(
          listOf(
            Label.parse("//included_target1"),
            Label.parse("//included_target2"),
            Label.parse("//included_target3"),
          ),
          listOf(Label.parse("//excluded_target1")),
        )
      workspaceContext.targets shouldBe expectedTargets

      val expectedDirectories =
        DirectoriesSpec(
          values =
            listOf(
              workspaceRoot.resolve("path/to/included1"),
              workspaceRoot.resolve("path/to/included2"),
              projectViewFilePath,
            ),
          excludedValues =
            listOf(
              workspaceRoot.resolve("path/to/excluded"),
            ),
        )
      workspaceContext.directories shouldBe expectedDirectories

      val expectedBuildFlagsSpec =
        BuildFlagsSpec(
          listOf(
            "--build_flag1=value1",
            "--build_flag2=value2",
            "--build_flag3=value3",
          ),
        )
      workspaceContext.buildFlags shouldBe expectedBuildFlagsSpec

      val expectedSyncFlagsSpec =
        SyncFlagsSpec(
          listOf(
            "--sync_flag1=value1",
            "--sync_flag2=value2",
            "--sync_flag3=value3",
          ),
        )
      workspaceContext.syncFlags shouldBe expectedSyncFlagsSpec

      val expectedBazelBinarySpec = BazelBinarySpec(Path("/path/to/bazel"))
      workspaceContext.bazelBinary shouldBe expectedBazelBinarySpec

      val expectedDotBazelBspDirPathSpec = DotBazelBspDirPathSpec(Path("path/to/bazelbsp"))
      workspaceContext.dotBazelBspDirPath shouldBe expectedDotBazelBspDirPathSpec

      val expectedAllowManualTargetsSyncSpec = AllowManualTargetsSyncSpec(false)
      workspaceContext.allowManualTargetsSync shouldBe expectedAllowManualTargetsSyncSpec

      val expectedImportDepthSpec = ImportDepthSpec(3)
      workspaceContext.importDepth shouldBe expectedImportDepthSpec

      val expectedEnabledRules = EnabledRulesSpec(listOf("rules_scala"))
      workspaceContext.enabledRules shouldBe expectedEnabledRules
    }
  }
}
