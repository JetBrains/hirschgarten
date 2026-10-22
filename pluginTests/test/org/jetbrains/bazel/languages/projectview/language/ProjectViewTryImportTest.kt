package org.jetbrains.bazel.languages.projectview.language

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
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Tests for ProjectView import logic focusing on try_import and missing import cases.
 */
@BazelTestApplication
class ProjectViewTryImportTest {

  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project
  private val rootDir get() = myFixture.testBazel.workspaceRoot

  @Test
  fun `test try_import missing file is ignored`() {
    val main =
      myFixture.testBazel.writeFile(
        "Main.bazelproject",
        """
        directories:
          dirB

        try_import Missing.bazelproject
        """.trimIndent(),
      ).toNioPath()

    val projectView = parse(main)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirB")))
  }

  @Test
  fun `test unresolved try_import mapping`() {
    val main =
      myFixture.testBazel.writeFile(
        "Main.bazelproject",
        """
        directories:
          dirB

        try_import Missing.bazelproject
        """.trimIndent(),
      ).toNioPath()

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
    myFixture.testBazel.writeFile(
      "Imported.bazelproject",
      """
      directories:
        dirA
      """.trimIndent(),
    )

    val main =
      myFixture.testBazel.writeFile(
        "Main.bazelproject",
        """
        try_import Imported.bazelproject

        directories:
          dirB
        """.trimIndent(),
      ).toNioPath()

    val projectView = parse(main)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `test try_import self reference does not recurse infinitely`() {
    val main = myFixture.testBazel.writeFile(
      "Main.bazelproject",
      """
        directories:
          dirA

        try_import Main.bazelproject
        """.trimIndent(),
    ).toNioPath()
    val projectView = parse(main)
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
    projectView.imports.shouldBeSingleton {
      it.shouldBeInstanceOf<Import.Resolved>()
    }
  }

  @Test
  fun `test try_import cycle back to root terminates without duplicating sections`() {
    myFixture.testBazel.writeFile(
      "Imported.bazelproject",
      """
      directories:
        dirB

      try_import Main.bazelproject
      """.trimIndent(),
    )
    val main =
      myFixture.testBazel.writeFile(
        "Main.bazelproject",
        """
        directories:
          dirA

        try_import Imported.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = parse(main)
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(
      ExcludableValue.included(Path("dirA")),
      ExcludableValue.included(Path("dirB")),
    )
  }

  private fun parse(source: Path): ProjectView = ProjectViewFactory.from(project, source = source, root = rootDir)
}
