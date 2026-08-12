package org.jetbrains.bazel.languages.projectview.language

import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.intellij.lang.annotations.Language
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.IMPORT_DEPTH_KEY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.languages.projectview.dotIdeaDirectoryLocation
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path


@BazelTestApplication
class ProjectViewContentTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDirFixture = tempPathFixture()
  private val moduleFixture = projectFixture.moduleFixture(tempDirFixture, addPathToSourceRoot = true)

  private val project get() = projectFixture.get()
  private val rootDir by tempDirFixture

  @BeforeEach
  fun setUp() {
    moduleFixture.get()
    initializeBazelProject(project, rootDir)
  }

  @Test
  fun `test sections are parsed from content with no file behind it`() {
    val projectView = parse(
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
    val projectView = parse(
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
    val source = rootDir.resolve("main.bazelproject")

    val projectView = parse(
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
    rootDir.writeProjectViewFile(
      "imported.bazelproject",
      """
      directories:
        dirA
      """.trimIndent(),
    )

    val projectView = parse("import imported.bazelproject")

    projectView.imports.single().shouldBeInstanceOf<Import.Resolved>().path shouldBe rootDir.resolve("imported.bazelproject")
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
  }

  private fun parse(@Language("projectview") content: String, source: Path? = null): ProjectView =
    ProjectViewFactory.from(project, content = content, root = rootDir, source = source)
}
