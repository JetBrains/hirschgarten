package org.jetbrains.bazel.workspacecontext.provider

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewImportDepthSection
import org.jetbrains.bazel.workspacecontext.ImportDepthSpec
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ImportDepthSpecMapperTest {
  @Nested
  @DisplayName("fun map(projectView): ImportDepthSpec tests")
  inner class MapTest {
    @Test
    fun `should return success with default import depth for empty project view`() {
      // given
      val projectView = ProjectView.Builder().build()

      // when
      val importDepthSpec = ImportDepthSpecExtractor.fromProjectView(projectView)

      // then
      val expectedImportDepthSpec = ImportDepthSpec(-1)
      importDepthSpec shouldBe expectedImportDepthSpec
    }

    @Test
    fun `should return success with mapped import depth from project view`() {
      // given
      val projectView =
        ProjectView
          .Builder(
            importDepth = ProjectViewImportDepthSection(3),
          ).build()
      // when
      val importDepthSpec = ImportDepthSpecExtractor.fromProjectView(projectView)
      // then
      val expectedImportDepthSpec = ImportDepthSpec(3)
      importDepthSpec shouldBe expectedImportDepthSpec
    }
  }
}
