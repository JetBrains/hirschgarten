package org.jetbrains.bazel.languages.projectview.language

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.IMPORT_DEPTH_KEY
import org.jetbrains.bazel.languages.projectview.dotIdeaDirectoryLocation
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.parseProjectView
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test
import kotlin.io.path.Path


@BazelTestApplication
class ProjectViewContentTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `test sections are parsed from content with no file behind it`() {
    val projectView = testBazel.parseProjectView(
      """
      dot_idea_directory_location: custom/.idea
      import_depth: 42
      directories:
        dirA
        -dirB
      """.trimIndent(),
    )

    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 42
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(
      ExcludableValue.included(Path("dirA")),
      ExcludableValue.excluded(Path("dirB")),
    )
  }

  @Test
  fun `test an unresolved import in content with no source has no position`() {
    val projectView = testBazel.parseProjectView(
      """
      dot_idea_directory_location: custom/.idea

      import missing.bazelproject
      """.trimIndent(),
    )

    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
    val unresolved = projectView.imports.single().shouldBeInstanceOf<Import.Unresolved>()
    unresolved.text shouldBe "missing.bazelproject"

    unresolved.position.shouldBeNull()
  }

  @Test
  fun `test an unresolved import is positioned when a source is passed alongside content`() {
    val source = testBazel.workspaceRoot.resolve("main.bazelproject")

    val projectView = testBazel.parseProjectView(
      """
      dot_idea_directory_location: custom/.idea

      import missing.bazelproject
      """.trimIndent(),
      source = source,
    )

    val unresolved = projectView.imports.single().shouldBeInstanceOf<Import.Unresolved>()
    val position = unresolved.position.shouldNotBeNull()
    position.path shouldBe source
    position.startLine shouldBe 2
    position.startColumn shouldBe 7
  }

  @Test
  fun `test imports in content are resolved against the root`() {
    testBazel.writeFile(
      "imported.bazelproject",
      """
      directories:
        dirA
      """.trimIndent(),
    )

    val projectView = testBazel.parseProjectView("import imported.bazelproject")

    val imported = projectView.imports.single().shouldBeInstanceOf<Import.Resolved>()
    imported.path shouldBe testBazel.workspaceRoot.resolve("imported.bazelproject")
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
  }
}
