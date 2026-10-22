package org.jetbrains.bazel.languages.projectview.language

import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.IMPORT_DEPTH_KEY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.languages.projectview.SHARD_SYNC_KEY
import org.jetbrains.bazel.languages.projectview.TARGETS_KEY
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

@BazelTestApplication
class ProjectViewImportTest {
  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project
  private val rootDir get() = myFixture.testBazel.workspaceRoot

  @Test
  fun `test import no overlap`() {
    myFixture.testBazel.writeFile(
      "A.bazelproject",
      """
      import_depth: 123
      targets:
        targetA
        targetB
      """.trimIndent(),
    )
    val main =
      myFixture.testBazel.writeFile(
        "B.bazelproject",
        """
        shard_sync: false
        directories:
          dirA
          dirB

        import A.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = parse(main)
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
    myFixture.testBazel.writeFile(
      "A.bazelproject",
      """
      import_depth: 123
      targets:
        targetA
        targetB
      """.trimIndent(),
    )
    val main =
      myFixture.testBazel.writeFile(
        "B.bazelproject",
        """
        import_depth: 321
        targets:
          targetC
          targetD

        import A.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = parse(main)
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
    myFixture.testBazel.writeFile(
      "A.bazelproject",
      """
      import_depth: 123
      targets:
        targetA
        targetB
      """.trimIndent(),
    )
    val main =
      myFixture.testBazel.writeFile(
        "B.bazelproject",
        """
        import A.bazelproject

        import_depth: 321
        targets:
          targetC
          targetD
        """.trimIndent(),
      ).toNioPath()
    val projectView = parse(main)
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
    val importedFile = myFixture.testBazel.writeFile("A.bazelproject", "import_depth: 1").toNioPath()
    val main = myFixture.testBazel.writeFile("B.bazelproject", "import A.bazelproject").toNioPath()
    val projectView = parse(main)
    projectView.imports.shouldBeSingleton {
      val resolved = it.shouldBeInstanceOf<Import.Resolved>()
      resolved.path shouldBe importedFile
      resolved.isRequired shouldBe true
    }
  }

  @Test
  fun `test unresolved import mapping`() {
    val main =
      myFixture.testBazel.writeFile(
        "B.bazelproject",
        """
        directories:
          dirA

        import missing.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = parse(main)
    projectView.imports.shouldBeSingleton {
      val unresolved = it.shouldBeInstanceOf<Import.Unresolved>()
      unresolved.isRequired shouldBe true
      unresolved.text shouldBe "missing.bazelproject"
      val position = unresolved.position.shouldNotBeNull()
      position.startLine shouldBe 3
      position.startColumn shouldBe 7
    }
  }

  @Test
  fun `test other sections are mapped when import is missing`() {
    val main =
      myFixture.testBazel.writeFile(
        "B.bazelproject",
        """
        import_depth: 42
        directories:
          dirA
          dirB

        import missing.bazelproject
        """.trimIndent(),
      ).toNioPath()
    val projectView = parse(main)
    projectView.imports.shouldBeSingleton {
      it.shouldBeInstanceOf<Import.Unresolved>()
    }
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 42
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `import should work with subdirectories`() {
    myFixture.testBazel.writeFile(
      "subdirectory/imported.bazelproject",
      """
      directories:
        dirA
      """.trimIndent(),
    )

    val main =
      myFixture.testBazel.writeFile(
        "main.bazelproject",
        """
        import subdirectory/imported.bazelproject
        """.trimIndent(),
      ).toNioPath()

    val projectView = parse(main)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
  }

  @Test
  fun `test self import does not recurse infinitely nor duplicate sections`() {
    val main = myFixture.testBazel.writeFile(
      "A.bazelproject",
      """
        targets:
          targetA

        import A.bazelproject
        """.trimIndent(),
    ).toNioPath()
    val projectView = parse(main)
    projectView.getSection(TARGETS_KEY) shouldContainExactly listOf(ExcludableValue.included(Label.parse("targetA")))
    projectView.imports.shouldBeSingleton {
      it.shouldBeInstanceOf<Import.Resolved>()
    }
  }

  @Test
  fun `test import cycle back to root terminates without duplicating sections`() {
    myFixture.testBazel.writeFile(
      "B.bazelproject",
      """
      targets:
        targetB

      import A.bazelproject
      """.trimIndent(),
    )
    val main = myFixture.testBazel.writeFile(
      "A.bazelproject",
      """
        targets:
          targetA

        import B.bazelproject
        """.trimIndent(),
    ).toNioPath()
    val projectView = parse(main)
    projectView.getSection(TARGETS_KEY) shouldContainExactly listOf(
      ExcludableValue.included(Label.parse("targetA")),
      ExcludableValue.included(Label.parse("targetB")),
    )
  }

  private fun parse(source: Path): ProjectView = ProjectViewFactory.from(project, source = source, root = rootDir)
}
