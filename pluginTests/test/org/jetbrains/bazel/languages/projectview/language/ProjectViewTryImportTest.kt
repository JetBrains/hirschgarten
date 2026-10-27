package org.jetbrains.bazel.languages.projectview.language

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.parseProjectView
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

/**
 * Tests for ProjectView import logic focusing on try_import and missing import cases.
 */
@BazelTestApplication
class ProjectViewTryImportTest {

  private val testBazel by testBazelProjectFixture()

  @Test
  fun `test try_import missing file is ignored`() {
    val main =
      testBazel.writeFile(
        "Main.bazelproject",
        """
        directories:
          dirB

        try_import Missing.bazelproject
        """.trimIndent(),
      ).toNioPath()

    val projectView = testBazel.parseProjectView(main)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirB")))
  }

  @Test
  fun `test unresolved try_import is not in the project view`() {
    val main =
      testBazel.writeFile(
        "Main.bazelproject",
        """
        directories:
          dirB

        try_import Missing.bazelproject
        """.trimIndent(),
      ).toNioPath()

    val projectView = testBazel.parseProjectView(main)

    projectView.imports.shouldBeEmpty()
  }

  @Test
  fun `test try_import present merges collections`() {
    testBazel.writeFile(
      "Imported.bazelproject",
      """
      directories:
        dirA
      """.trimIndent(),
    )

    val main =
      testBazel.writeFile(
        "Main.bazelproject",
        """
        try_import Imported.bazelproject

        directories:
          dirB
        """.trimIndent(),
      ).toNioPath()

    val projectView = testBazel.parseProjectView(main)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `test try_import self reference does not recurse infinitely`() {
    val main = testBazel.writeFile(
      "Main.bazelproject",
      """
        directories:
          dirA

        try_import Main.bazelproject
        """.trimIndent(),
    ).toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
    projectView.imports.shouldBeSingleton {
      it.shouldBeInstanceOf<Import.Resolved>()
    }
  }

  @Test
  fun `test try_import cycle back to root terminates without duplicating sections`() {
    testBazel.writeFile(
      "Imported.bazelproject",
      """
      directories:
        dirB

      try_import Main.bazelproject
      """.trimIndent(),
    )
    val main =
      testBazel.writeFile(
        "Main.bazelproject",
        """
        directories:
          dirA

        try_import Imported.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(
      ExcludableValue.included(Path("dirA")),
      ExcludableValue.included(Path("dirB")),
    )
  }
}
