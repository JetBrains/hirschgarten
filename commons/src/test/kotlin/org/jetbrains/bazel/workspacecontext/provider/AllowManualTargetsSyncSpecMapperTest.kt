package org.jetbrains.bazel.workspacecontext.provider

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewAllowManualTargetsSyncSection
import org.jetbrains.bazel.workspacecontext.AllowManualTargetsSyncSpec
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AllowManualTargetsSyncSpecMapperTest {
  @Nested
  @DisplayName("fun map(projectView): AllowManualTargetsSyncSpec tests")
  inner class MapTest {
    @Test
    fun `should return success with default spec if build manual targets is null`() {
      // given
      val projectView = ProjectView.Builder(allowManualTargetsSync = null).build()

      // when
      val allowManualTargetsSyncSpec = AllowManualTargetsSyncSpecExtractor.fromProjectView(projectView)

      // then
      val expectedAllowManualTargetsSyncSpec = AllowManualTargetsSyncSpec(false)
      allowManualTargetsSyncSpec shouldBe expectedAllowManualTargetsSyncSpec
    }

    @Test
    fun `should return success for successful mapping`() {
      // given
      val projectView =
        ProjectView
          .Builder(
            allowManualTargetsSync =
              ProjectViewAllowManualTargetsSyncSection(
                true,
              ),
          ).build()

      // when
      val allowManualTargetsSyncSpec = AllowManualTargetsSyncSpecExtractor.fromProjectView(projectView)

      // then
      val expectedAllowManualTargetsSyncSpec =
        AllowManualTargetsSyncSpec(
          true,
        )
      allowManualTargetsSyncSpec shouldBe expectedAllowManualTargetsSyncSpec
    }
  }
}
