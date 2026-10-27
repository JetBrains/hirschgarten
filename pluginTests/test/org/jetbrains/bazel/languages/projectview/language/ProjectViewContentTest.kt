package org.jetbrains.bazel.languages.projectview.language

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.intellij.lang.annotations.Language
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.IMPORT_DEPTH_KEY
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
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
  fun `test an unresolved import in content with no source has no path`() {
    val problems = mutableListOf<ProjectViewProblem>()
    val projectView = testBazel.parseProjectView(
      """
      dot_idea_directory_location: custom/.idea

      import missing.bazelproject
      """.trimIndent(),
      onEachProblem = problems::add,
    )

    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
    projectView.imports.shouldBeEmpty()
    problems.single().sourcePath.shouldBeNull()
  }

  @Test
  fun `test an unresolved import has the path of the source that is passed alongside content`() {
    val source = testBazel.workspaceRoot.resolve("main.bazelproject")
    val problems = mutableListOf<ProjectViewProblem>()

    val projectView = testBazel.parseProjectView(
      """
      dot_idea_directory_location: custom/.idea

      import missing.bazelproject
      """.trimIndent(),
      source = source,
      onEachProblem = problems::add,
    )

    projectView.imports.shouldBeEmpty()
    problems.single().sourcePath shouldBe source
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

  @Test
  fun `test the problems of the content are reported`() {
    problems(
      """
      shard_sync: not_a_boolean
      not_supported_section: someValue
      import missing.bazelproject
      try_import optional.bazelproject
      """.trimIndent(),
    ).map { it.id } shouldContainExactly listOf(
      ProjectViewProblem.Id.InvalidValue,
      ProjectViewProblem.Id.UnsupportedSection,
      ProjectViewProblem.Id.UnresolvedImport,
    )
  }

  @Test
  fun `test the default template has no problems`() {
    problems(ProjectViewFileUtils.projectViewTemplate(testBazel.project.rootDir, forceDefaultTemplate = true)).shouldBeEmpty()
  }

  @Test
  fun `test a suppression comment covers the next section`() {
    problems(
      """
      # suppress: unsupported_section
      not_supported_section: someValue
      """.trimIndent(),
    ).shouldBeEmpty()
  }

  @Test
  fun `test a suppression comment covers the items of the next section`() {
    problems(
      """
      # suppress: unknown_flag
      build_flags:
        not_a_flag
      """.trimIndent(),
    ).shouldBeEmpty()
  }

  @Test
  fun `test a suppression comment in a section covers only the next item`() {
    problems(
      """
      build_flags:
        not_a_flag_a
        # suppress: unknown_flag
        not_a_flag_b
        not_a_flag_c
      """.trimIndent(),
    ).map { it.element?.text } shouldContainExactly listOf("not_a_flag_a", "not_a_flag_c")
  }

  @Test
  fun `test a suppression comment after a section covers the next section`() {
    problems(
      """
      directories:
        dirA
      # suppress: unsupported_section
      not_supported_section: someValue
      """.trimIndent(),
    ).shouldBeEmpty()
  }

  @Test
  fun `test a suppression comment takes several IDs and free text`() {
    problems(
      """
      #suppress:unknown_flag , unsupported_section the CI image needs it, deprecation
      not_supported_section: someValue
      """.trimIndent(),
    ).shouldBeEmpty()
  }

  @Test
  fun `test an ID in the free text of a suppression comment suppresses nothing`() {
    problems(
      """
      # suppress: unknown_flag - unsupported_section
      not_supported_section: someValue
      """.trimIndent(),
    ).map { it.id } shouldContainExactly listOf(ProjectViewProblem.Id.UnsupportedSection)
  }

  @Test
  fun `test a suppression comment does not cover a section after a blank line`() {
    problems(
      """
      # suppress: unsupported_section

      not_supported_section: someValue
      """.trimIndent(),
    ).map { it.id } shouldContainExactly listOf(ProjectViewProblem.Id.UnsupportedSection)
  }

  @Test
  fun `test a suppression comment with another ID suppresses nothing`() {
    problems(
      """
      # suppress: unknown_flag
      not_supported_section: someValue
      """.trimIndent(),
    ).map { it.id } shouldContainExactly listOf(ProjectViewProblem.Id.UnsupportedSection)
  }

  @Test
  fun `test a suppression comment does not suppress an error`() {
    problems(
      """
      # suppress: invalid_value
      shard_sync: not_a_boolean
      """.trimIndent(),
    ).map { it.id } shouldContainExactly listOf(ProjectViewProblem.Id.InvalidValue)
  }

  @Test
  fun `test a problem ID cannot be written in a suppression comment unless it is a word`() {
    shouldThrow<IllegalArgumentException> { ProjectViewProblem.Id("not-an-id") }
  }

  private fun problems(@Language("projectview") content: String): List<ProjectViewProblem> {
    val problems = mutableListOf<ProjectViewProblem>()
    testBazel.parseProjectView(content, onEachProblem = problems::add)
    return problems
  }
}
