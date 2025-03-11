package org.jetbrains.bazel.workspacecontext.provider

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBuildFlagsSection
import org.jetbrains.bazel.workspacecontext.BuildFlagsSpec
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BuildFlagsSpecMapperTest {
  @Nested
  @DisplayName("fun map(projectView): BuildFlagsSpec tests")
  inner class MapTest {
    @Test
    fun `should return success with default spec if build flags are null`() {
      // given
      val projectView = ProjectView.Builder(buildFlags = null).build()

      // when
      val buildFlagsSpec = BuildFlagsSpecExtractor.fromProjectView(projectView)

      // then
      val expectedBuildFlagsSpec = BuildFlagsSpec(emptyList())
      buildFlagsSpec shouldBe expectedBuildFlagsSpec
    }

    @Test
    fun `should return success for successful mapping`() {
      // given
      val projectView =
        ProjectView
          .Builder(
            buildFlags =
              ProjectViewBuildFlagsSection(
                listOf(
                  "--build_flag1=value1",
                  "--build_flag2=value2",
                  "--build_flag3=value3",
                ),
              ),
          ).build()

      // when
      val buildFlagsSpec = BuildFlagsSpecExtractor.fromProjectView(projectView)

      // then
      val expectedBuildFlagsSpec =
        BuildFlagsSpec(
          listOf(
            "--build_flag1=value1",
            "--build_flag2=value2",
            "--build_flag3=value3",
          ),
        )
      buildFlagsSpec shouldBe expectedBuildFlagsSpec
    }
  }
}
