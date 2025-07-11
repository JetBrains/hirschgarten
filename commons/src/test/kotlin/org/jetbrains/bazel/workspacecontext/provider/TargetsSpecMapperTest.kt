package org.jetbrains.bazel.workspacecontext.provider

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDeriveTargetsFromDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewTargetsSection
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContextEntityExtractorException
import org.jetbrains.bazel.workspacecontext.provider.TargetsSpecExtractor
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class TargetsSpecMapperTest {
  @Nested
  @DisplayName("fun map(projectView): TargetsSpec tests")
  inner class MapTest {
    @Nested
    @DisplayName("without derive_targets_from_targets")
    inner class MapTestWithoutderiveTargetsFromDirectoriesSection {
      @Test
      fun `should return success with default spec if targets are null`() {
        // given
        val projectView = ProjectView.Builder(targets = null).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargetsSpec = TargetsSpec(emptyList(), emptyList())
        targetsSpec shouldBe expectedTargetsSpec
      }

      @Test
      fun `should return success and default if targets have no values`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets =
                ProjectViewTargetsSection(
                  emptyList(),
                  emptyList(),
                ),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargetsSpec = TargetsSpec(emptyList(), emptyList())
        targetsSpec shouldBe expectedTargetsSpec
      }

      @Test
      fun `should return fail if targets have no included values`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets =
                ProjectViewTargetsSection(
                  emptyList(),
                  listOf(
                    Label.parse("//excluded_target1"),
                    Label.parse("//excluded_target2"),
                  ),
                ),
            ).build()

        // when
        val exception =
          shouldThrow<WorkspaceContextEntityExtractorException> {
            TargetsSpecExtractor.fromProjectView(projectView)
          }

        // then
        exception.message shouldBe "Mapping project view into 'targets' failed! 'targets' section has no included targets."
      }

      @Test
      fun `should return success for successful mapping`() {
        // given
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
                  listOf(
                    Label.parse("//excluded_target1"),
                    Label.parse("//excluded_target2"),
                  ),
                ),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargets =
          TargetsSpec(
            listOf(
              Label.parse("//included_target1"),
              Label.parse("//included_target2"),
              Label.parse("//included_target3"),
            ),
            listOf(
              Label.parse("//excluded_target1"),
              Label.parse("//excluded_target2"),
            ),
          )
        targetsSpec shouldBe expectedTargets
      }
    }

    @Nested
    @DisplayName("with derive_targets_from_targets false")
    inner class MapTestWithderiveTargetsFromDirectoriesSectionFalse {
      @Test
      fun `should return success with default spec if targets are null and flag is false`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets = null,
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(false),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargetsSpec = TargetsSpec(emptyList(), emptyList())
        targetsSpec shouldBe expectedTargetsSpec
      }

      @Test
      fun `should return success and default if targets have no values and flag is false`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets =
                ProjectViewTargetsSection(
                  emptyList(),
                  emptyList(),
                ),
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(false),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargetsSpec = TargetsSpec(emptyList(), emptyList())
        targetsSpec shouldBe expectedTargetsSpec
      }

      @Test
      fun `should return fail if targets have no included values and flag is False`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets =
                ProjectViewTargetsSection(
                  emptyList(),
                  listOf(
                    Label.parse("//excluded_target1"),
                    Label.parse("//excluded_target2"),
                  ),
                ),
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(false),
            ).build()

        // when
        val exception =
          shouldThrow<WorkspaceContextEntityExtractorException> {
            TargetsSpecExtractor.fromProjectView(projectView)
          }

        // then
        exception.message shouldBe "Mapping project view into 'targets' failed! 'targets' section has no included targets."
      }

      @Test
      fun `should return success for successful mapping`() {
        // given
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
                  listOf(
                    Label.parse("//excluded_target1"),
                    Label.parse("//excluded_target2"),
                  ),
                ),
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(false),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargets =
          TargetsSpec(
            listOf(
              Label.parse("//included_target1"),
              Label.parse("//included_target2"),
              Label.parse("//included_target3"),
            ),
            listOf(
              Label.parse("//excluded_target1"),
              Label.parse("//excluded_target2"),
            ),
          )
        targetsSpec shouldBe expectedTargets
      }
    }

    @Nested
    @DisplayName("with derive_targets_from_targets true")
    inner class MapTestWithderiveTargetsFromDirectoriesSectionTrue {
      @Test
      fun `should return success with default spec if targets and directories are null and flag is true`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets = null,
              directories = null,
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(true),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargetsSpec = TargetsSpec(emptyList(), emptyList())
        targetsSpec shouldBe expectedTargetsSpec
      }

      @Test
      fun `should return success and default if targets and directories have no values and flag is true`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets =
                ProjectViewTargetsSection(
                  emptyList(),
                  emptyList(),
                ),
              directories =
                ProjectViewDirectoriesSection(
                  emptyList(),
                  emptyList(),
                ),
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(true),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargetsSpec = TargetsSpec(emptyList(), emptyList())
        targetsSpec shouldBe expectedTargetsSpec
      }

      @Test
      fun `should return fail if directories have no included values and flag is True`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets =
                ProjectViewTargetsSection(
                  emptyList(),
                  listOf(
                    Label.parse("//excluded_target1"),
                    Label.parse("//excluded_target2"),
                  ),
                ),
              directories =
                ProjectViewDirectoriesSection(
                  emptyList(),
                  listOf(
                    Path("excluded_target1"),
                    Path("excluded_target2"),
                  ),
                ),
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(true),
            ).build()

        // when
        val exception =
          shouldThrow<WorkspaceContextEntityExtractorException> {
            TargetsSpecExtractor.fromProjectView(projectView)
          }

        // then
        exception.message shouldBe "Mapping project view into 'targets' failed! 'directories' section has no included targets."
      }

      @Test
      fun `should return success for successful mapping with empty targets`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets =
                ProjectViewTargetsSection(
                  emptyList(),
                  emptyList(),
                ),
              directories =
                ProjectViewDirectoriesSection(
                  listOf(
                    Path("included_dir1"),
                    Path("included_dir2/"),
                    Path("included_dir3"),
                  ),
                  listOf(
                    Path("excluded_dir1"),
                    Path("excluded_dir2"),
                  ),
                ),
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(true),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargets =
          TargetsSpec(
            listOf(
              Label.parse("//included_dir1/..."),
              Label.parse("//included_dir2/..."),
              Label.parse("//included_dir3/..."),
            ),
            listOf(
              Label.parse("//excluded_dir1/..."),
              Label.parse("//excluded_dir2/..."),
            ),
          )
        targetsSpec shouldBe expectedTargets
      }

      @Test
      fun `should return success for successful mapping with non-empty targets`() {
        // given
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
                  listOf(
                    Label.parse("//excluded_target1"),
                    Label.parse("//excluded_target2"),
                  ),
                ),
              directories =
                ProjectViewDirectoriesSection(
                  listOf(
                    Path("included_dir1"),
                    Path("included_dir2/"),
                    Path("included_dir3"),
                  ),
                  listOf(
                    Path("excluded_dir1"),
                    Path("excluded_dir2"),
                  ),
                ),
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(true),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargets =
          TargetsSpec(
            listOf(
              Label.parse("//included_target1"),
              Label.parse("//included_target2"),
              Label.parse("//included_target3"),
              Label.parse("//included_dir1/..."),
              Label.parse("//included_dir2/..."),
              Label.parse("//included_dir3/..."),
            ),
            listOf(
              Label.parse("//excluded_target1"),
              Label.parse("//excluded_target2"),
              Label.parse("//excluded_dir1/..."),
              Label.parse("//excluded_dir2/..."),
            ),
          )
        targetsSpec shouldBe expectedTargets
      }

      @Test
      fun `should return success for successful mapping with different directory cases`() {
        // given
        val projectView =
          ProjectView
            .Builder(
              targets =
                ProjectViewTargetsSection(
                  emptyList(),
                  emptyList(),
                ),
              directories =
                ProjectViewDirectoriesSection(
                  listOf(
                    Path("included_dir1/"),
                    Path("included_dir2/"),
                    Path("."),
                  ),
                  listOf(
                    Path("excluded_dir1/"),
                    Path("excluded_dir2/"),
                  ),
                ),
              deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSection(true),
            ).build()

        // when
        val targetsSpec = TargetsSpecExtractor.fromProjectView(projectView)

        // then
        val expectedTargets =
          TargetsSpec(
            listOf(
              Label.parse("//included_dir1/..."),
              Label.parse("//included_dir2/..."),
              Label.parse("//..."),
            ),
            listOf(
              Label.parse("//excluded_dir1/..."),
              Label.parse("//excluded_dir2/..."),
            ),
          )
        targetsSpec shouldBe expectedTargets
      }
    }
  }
}
