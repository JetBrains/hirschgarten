package org.jetbrains.bazel.languages.projectview.language

import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Tests for ProjectView import logic focusing on try_import and missing import cases.
 */
@BazelTestApplication
class ProjectViewTryImportTest {

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
  fun `test try_import missing file is ignored`() {
    val main =
      rootDir.writeProjectViewFile(
        "Main.bazelproject",
        """
        directories:
          dirB

        try_import Missing.bazelproject
        """.trimIndent(),
      )

    val projectView = parse(main)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirB")))
  }

  @Test
  fun `test unresolved try_import mapping`() {
    val main =
      rootDir.writeProjectViewFile(
        "Main.bazelproject",
        """
        directories:
          dirB

        try_import Missing.bazelproject
        """.trimIndent(),
      )

    val projectView = parse(main)

    projectView.imports.shouldBeSingleton {
      val unresolved = it.shouldBeInstanceOf<Import.Unresolved>()
      unresolved.isRequired shouldBe false
      unresolved.text shouldBe "Missing.bazelproject"
      val position = unresolved.position.shouldNotBeNull()
      position.startLine shouldBe 3
      position.startColumn shouldBe 11
    }
  }

  @Test
  fun `test try_import present merges collections`() {
    rootDir.writeProjectViewFile(
      "Imported.bazelproject",
      """
      directories:
        dirA
      """.trimIndent(),
    )

    val main =
      rootDir.writeProjectViewFile(
        "Main.bazelproject",
        """
        try_import Imported.bazelproject

        directories:
          dirB
        """.trimIndent(),
      )

    val projectView = parse(main)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `test try_import self reference does not recurse infinitely`() {
    val main = rootDir.writeProjectViewFile(
      "Main.bazelproject",
      """
        directories:
          dirA

        try_import Main.bazelproject
        """.trimIndent(),
    )
    val projectView = parse(main)
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
    projectView.imports.shouldBeSingleton {
      it.shouldBeInstanceOf<Import.Resolved>()
    }
  }

  @Test
  fun `test try_import cycle back to root terminates without duplicating sections`() {
    rootDir.writeProjectViewFile(
      "Imported.bazelproject",
      """
      directories:
        dirB

      try_import Main.bazelproject
      """.trimIndent(),
    )
    val main =
      rootDir.writeProjectViewFile(
        "Main.bazelproject",
        """
        directories:
          dirA

        try_import Imported.bazelproject
        """.trimIndent(),
      )
    val projectView = parse(main)
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(
      ExcludableValue.included(Path("dirA")),
      ExcludableValue.included(Path("dirB")),
    )
  }

  private fun parse(source: Path): ProjectView = ProjectViewFactory.from(project, source = source, root = rootDir)
}
