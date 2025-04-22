package org.jetbrains.bazel.workspacecontext.provider

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDirectoriesSection
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class DirectoriesSpecExtractorTest {
  val workspaceRoot = Path("path/to/workspace")
  val projectViewFilePath = workspaceRoot.resolve("path/to/project/view/file")

  @Test
  fun `should return workspace root + project view file path if directories section is null`() {
    // given
    val projectView = ProjectView.Builder(directories = null).build()

    // when
    val directoriesSpec = DirectoriesSpecExtractor(workspaceRoot, projectViewFilePath).fromProjectView(projectView)

    // then
    val expectedDirectoriesSpec =
      DirectoriesSpec(
        values = listOf(workspaceRoot, projectViewFilePath),
        excludedValues = emptyList(),
      )
    directoriesSpec shouldBe expectedDirectoriesSpec
  }

  @Test
  fun `should return workspace root + project view file path if directories section contains only dot wildcard`() {
    // given
    val projectView =
      ProjectView
        .Builder(
          directories =
            ProjectViewDirectoriesSection(
              values = listOf(Path(".")),
              excludedValues = emptyList(),
            ),
        ).build()

    // when
    val directoriesSpec = DirectoriesSpecExtractor(workspaceRoot, projectViewFilePath).fromProjectView(projectView)

    // then
    val expectedDirectoriesSpec =
      DirectoriesSpec(
        values = listOf(workspaceRoot, projectViewFilePath),
        excludedValues = emptyList(),
      )
    directoriesSpec shouldBe expectedDirectoriesSpec
  }

  @Test
  fun `should return resolved paths + project view file path if directories section is not null`() {
    // given
    val projectView =
      ProjectView
        .Builder(
          directories =
            ProjectViewDirectoriesSection(
              values = listOf(Path("path/to/included1"), Path("path/to/included2")),
              excludedValues = listOf(Path("path/to/excluded")),
            ),
        ).build()

    // when
    val directoriesSpec = DirectoriesSpecExtractor(workspaceRoot, projectViewFilePath).fromProjectView(projectView)

    // then
    val expectedDirectoriesSpec =
      DirectoriesSpec(
        values =
          listOf(
            workspaceRoot.resolve("path/to/included1"),
            workspaceRoot.resolve("path/to/included2"),
            projectViewFilePath,
          ),
        excludedValues = listOf(workspaceRoot.resolve("path/to/excluded")),
      )
    directoriesSpec shouldBe expectedDirectoriesSpec
  }

  @Test
  fun `should still include project view file path even if it is included in excluded values`() {
    // given
    val projectView =
      ProjectView
        .Builder(
          directories =
            ProjectViewDirectoriesSection(
              values = listOf(Path("path/to/included1"), Path("path/to/included2")),
              excludedValues = listOf(Path("path/to/excluded"), Path("path/to/project/view/file")),
            ),
        ).build()

    // when
    val directoriesSpec = DirectoriesSpecExtractor(workspaceRoot, projectViewFilePath).fromProjectView(projectView)

    // then
    val expectedDirectoriesSpec =
      DirectoriesSpec(
        values =
          listOf(
            workspaceRoot.resolve("path/to/included1"),
            workspaceRoot.resolve("path/to/included2"),
            projectViewFilePath,
          ),
        excludedValues = listOf(workspaceRoot.resolve("path/to/excluded")),
      )
    directoriesSpec shouldBe expectedDirectoriesSpec
  }
}
