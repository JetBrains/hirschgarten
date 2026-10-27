package org.jetbrains.bazel.languages.projectview.language

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.IMPORT_DEPTH_KEY
import org.jetbrains.bazel.languages.projectview.SHARD_SYNC_KEY
import org.jetbrains.bazel.languages.projectview.TARGETS_KEY
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.parseProjectView
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createSymbolicLinkPointingTo

@BazelTestApplication
class ProjectViewImportTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `test an import cycle through a symbolic link stops`() {
    val main = testBazel.writeFile(
      "main.bazelproject",
      """
      import link.bazelproject
      directories:
        dirA
      """.trimIndent(),
    ).toNioPath()
    testBazel.workspaceRoot.resolve("link.bazelproject").createSymbolicLinkPointingTo(main)

    val projectView = testBazel.parseProjectView(main)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
    projectView.imports.shouldBeSingleton { it.path shouldBe testBazel.workspaceRoot.resolve("link.bazelproject") }
  }

  @Test
  fun `test the last import of a file wins`() {
    testBazel.writeFile("shared.bazelproject", "shard_sync: false").toNioPath()
    testBazel.writeFile("other.bazelproject", "import shared.bazelproject").toNioPath()
    val main = testBazel.writeFile(
      "main.bazelproject",
      """
      import shared.bazelproject
      shard_sync: true
      import other.bazelproject
      """.trimIndent(),
    ).toNioPath()

    testBazel.parseProjectView(main).getSection(SHARD_SYNC_KEY) shouldBe false
  }

  @Test
  fun `test import no overlap`() {
    testBazel.writeFile(
      "A.bazelproject",
      """
      import_depth: 123
      targets:
        targetA
        targetB
      """.trimIndent(),
    )
    val main =
      testBazel.writeFile(
        "B.bazelproject",
        """
        shard_sync: false
        directories:
          dirA
          dirB

        import A.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 123
    projectView.getSection(SHARD_SYNC_KEY) shouldBe false
    projectView.getSection(TARGETS_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Label.parse("targetA")),
        ExcludableValue.included(Label.parse("targetB")),
      )
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `test import full overlap`() {
    testBazel.writeFile(
      "A.bazelproject",
      """
      import_depth: 123
      targets:
        targetA
        targetB
      """.trimIndent(),
    )
    val main =
      testBazel.writeFile(
        "B.bazelproject",
        """
        import_depth: 321
        targets:
          targetC
          targetD

        import A.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 123
    projectView.getSection(TARGETS_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Label.parse("targetC")),
        ExcludableValue.included(Label.parse("targetD")),
        ExcludableValue.included(Label.parse("targetA")),
        ExcludableValue.included(Label.parse("targetB")),
      )
  }

  @Test
  fun `test import full overlap import first`() {
    testBazel.writeFile(
      "A.bazelproject",
      """
      import_depth: 123
      targets:
        targetA
        targetB
      """.trimIndent(),
    )
    val main =
      testBazel.writeFile(
        "B.bazelproject",
        """
        import A.bazelproject

        import_depth: 321
        targets:
          targetC
          targetD
        """.trimIndent(),
      ).toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 321
    projectView.getSection(TARGETS_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Label.parse("targetA")),
        ExcludableValue.included(Label.parse("targetB")),
        ExcludableValue.included(Label.parse("targetC")),
        ExcludableValue.included(Label.parse("targetD")),
      )
  }

  @Test
  fun `test resolved import mapping`() {
    val importedFile = testBazel.writeFile("A.bazelproject", "import_depth: 1").toNioPath()
    val main = testBazel.writeFile("B.bazelproject", "import A.bazelproject").toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.imports.shouldBeSingleton {
      val resolved = it.shouldBeInstanceOf<Import.Resolved>()
      resolved.path shouldBe importedFile
      resolved.isRequired shouldBe true
    }
  }

  @Test
  fun `test unresolved import is not in the project view`() {
    val main =
      testBazel.writeFile(
        "B.bazelproject",
        """
        directories:
          dirA

        import missing.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.imports.shouldBeEmpty()
  }

  @Test
  fun `test other sections are mapped when import is missing`() {
    val main =
      testBazel.writeFile(
        "B.bazelproject",
        """
        import_depth: 42
        directories:
          dirA
          dirB

        import missing.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.imports.shouldBeEmpty()
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 42
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `import should work with subdirectories`() {
    testBazel.writeFile(
      "subdirectory/imported.bazelproject",
      """
      directories:
        dirA
      """.trimIndent(),
    )

    val main =
      testBazel.writeFile(
        "main.bazelproject",
        """
        import subdirectory/imported.bazelproject
        """.trimIndent(),
      ).toNioPath()

    val projectView = testBazel.parseProjectView(main)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
  }

  @Test
  fun `test self import does not recurse infinitely nor duplicate sections`() {
    val main = testBazel.writeFile(
      "A.bazelproject",
      """
        targets:
          targetA

        import A.bazelproject
        """.trimIndent(),
    ).toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.getSection(TARGETS_KEY) shouldContainExactly listOf(ExcludableValue.included(Label.parse("targetA")))
    projectView.imports.shouldBeSingleton {
      it.shouldBeInstanceOf<Import.Resolved>()
    }
  }

  @Test
  fun `test import cycle back to root terminates without duplicating sections`() {
    testBazel.writeFile(
      "B.bazelproject",
      """
      targets:
        targetB

      import A.bazelproject
      """.trimIndent(),
    )
    val main = testBazel.writeFile(
      "A.bazelproject",
      """
        targets:
          targetA

        import B.bazelproject
        """.trimIndent(),
    ).toNioPath()
    val projectView = testBazel.parseProjectView(main)
    projectView.getSection(TARGETS_KEY) shouldContainExactly listOf(
      ExcludableValue.included(Label.parse("targetA")),
      ExcludableValue.included(Label.parse("targetB")),
    )
  }

  @Test
  fun `test reports problems of imported files`() {
    val imported = testBazel.writeFile("imported.bazelproject", "shard_sync: not_a_boolean").toNioPath()
    val main = testBazel.writeFile("main.bazelproject", "import imported.bazelproject").toNioPath()

    val problem = problems(main).single()
    problem.id shouldBe ProjectViewProblem.Id.InvalidValue
    problem.sourcePath shouldBe imported
  }

  @Test
  fun `test reports a file that is imported two times only once`() {
    val shared = testBazel.writeFile("shared.bazelproject", "not_supported_section: someValue").toNioPath()
    testBazel.writeFile("a.bazelproject", "import shared.bazelproject").toNioPath()
    testBazel.writeFile("b.bazelproject", "import shared.bazelproject").toNioPath()
    val main = testBazel.writeFile(
      "main.bazelproject",
      """
      import a.bazelproject
      import b.bazelproject
      """.trimIndent(),
    ).toNioPath()

    problems(main).map { it.sourcePath } shouldContainExactly listOf(shared)
  }

  @Test
  fun `test reports a file that one file imports two times only once`() {
    val shared = testBazel.writeFile("shared.bazelproject", "not_supported_section: someValue").toNioPath()
    val main = testBazel.writeFile(
      "main.bazelproject",
      """
      import shared.bazelproject
      import shared.bazelproject
      """.trimIndent(),
    ).toNioPath()

    problems(main).map { it.sourcePath } shouldContainExactly listOf(shared)
  }

  @Test
  fun `test reports a file that is imported through a symbolic link only once`() {
    val shared = testBazel.writeFile("shared.bazelproject", "not_supported_section: someValue").toNioPath()
    testBazel.workspaceRoot.resolve("link.bazelproject").createSymbolicLinkPointingTo(shared)
    val main = testBazel.writeFile(
      "main.bazelproject",
      """
      import shared.bazelproject
      import link.bazelproject
      """.trimIndent(),
    ).toNioPath()

    problems(main).map { it.sourcePath } shouldContainExactly listOf(shared)
  }

  @Test
  fun `test problem reporting stops at an import cycle`() {
    testBazel.writeFile("a.bazelproject", "import main.bazelproject").toNioPath()
    val main = testBazel.writeFile("main.bazelproject", "import a.bazelproject").toNioPath()

    problems(main).shouldBeEmpty()
  }

  @Test
  fun `test a suppression comment in an imported file covers its problems`() {
    testBazel.writeFile(
      "imported.bazelproject",
      """
      # suppress: unsupported_section
      not_supported_section: someValue
      """.trimIndent(),
    ).toNioPath()
    val main = testBazel.writeFile("main.bazelproject", "import imported.bazelproject").toNioPath()

    problems(main).shouldBeEmpty()
  }

  private fun problems(path: Path): List<ProjectViewProblem> {
    val problems = mutableListOf<ProjectViewProblem>()
    testBazel.parseProjectView(path, problems::add)
    return problems
  }
}
