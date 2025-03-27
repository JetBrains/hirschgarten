package org.jetbrains.bazel.workspacecontext.provider

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewSyncFlagsSection
import org.jetbrains.bazel.workspacecontext.SyncFlagsSpec
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SyncFlagsSpecMapperTest {
  @Nested
  @DisplayName("fun map(projectView): SyncFlagsSpec tests")
  inner class MapTest {
    @Test
    fun `should return success with default spec if sync flags are null`() {
      // given
      val projectView = ProjectView.Builder(syncFlags = null).build()

      // when
      val syncFlagsSpec = SyncFlagsSpecExtractor.fromProjectView(projectView)

      // then
      syncFlagsSpec shouldBe SyncFlagsSpec(emptyList())
    }

    @Test
    fun `should return success for successful mapping`() {
      // given
      val projectView =
        ProjectView
          .Builder(
            syncFlags =
              ProjectViewSyncFlagsSection(
                listOf(
                  "--sync_flag1=value1",
                  "--sync_flag2=value2",
                  "--sync_flag3=value3",
                ),
              ),
          ).build()

      // when
      val syncFlagsSpec = SyncFlagsSpecExtractor.fromProjectView(projectView)

      // then
      val expectedSyncFlagsSpec =
        SyncFlagsSpec(
          listOf(
            "--sync_flag1=value1",
            "--sync_flag2=value2",
            "--sync_flag3=value3",
          ),
        )
      syncFlagsSpec shouldBe expectedSyncFlagsSpec
    }
  }
}
