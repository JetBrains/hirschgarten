package org.jetbrains.bazel.languages.projectview.language

import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.ModuleFixture
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.Path

/**
 * Tests for ProjectView import logic focusing on try_import and missing import cases.
 */
@RunWith(JUnit4::class)
class ProjectViewTryImportTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {
  override fun setUp() {
    super.setUp()
    initializeBazelProject(project, myFixture.tempDirPath)
  }

  @Test
  fun `test try_import missing file is ignored`() {
    // Ensure imports resolve from project root
    val psiFile =
      myFixture.configureByText(
        "Main.bazelproject",
        """
        directories:
          dirB
        
        try_import Missing.bazelproject
        """.trimIndent(),
      )

    val projectView = ProjectViewFactory.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirB")))
  }

  @Test
  fun `test unresolved try_import mapping`() {
    val psiFile =
      myFixture.configureByText(
        "Main.bazelproject",
        """
        directories:
          dirB

        try_import Missing.bazelproject
        """.trimIndent(),
      )

    val projectView = ProjectViewFactory.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)

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
    myFixture.addFileToProject(
      "Imported.bazelproject",
      """
      directories:
        dirA
      """.trimIndent(),
    )

    val psiFile =
      myFixture.configureByText(
        "Main.bazelproject",
        """
        try_import Imported.bazelproject
        
        directories:
          dirB
        """.trimIndent(),
      )

    val projectView = ProjectViewFactory.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)

    // Since try_import comes first, imported directories should appear before local ones
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `import should work with subdirectories`() {
    myFixture.addFileToProject(
      "subdirectory/imported.bazelproject",
      """
      directories:
        dirA
      """.trimIndent(),
    )

    val psiFile =
      myFixture.configureByText(
        "main.bazelproject",
        """
        import subdirectory/imported.bazelproject
        """.trimIndent(),
      )

    val projectView = ProjectViewFactory.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
  }

  @Test
  fun `test try_import self reference does not recurse infinitely`() {
    val psiFile = myFixture.addFileToProject(
      "Main.bazelproject",
      """
        directories:
          dirA

        try_import Main.bazelproject
        """.trimIndent(),
    )
    val projectView = ProjectViewFactory.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
    projectView.imports.shouldBeSingleton {
      it.shouldBeInstanceOf<Import.Resolved>()
    }
  }

  @Test
  fun `test try_import cycle back to root terminates without duplicating sections`() {
    myFixture.addFileToProject(
      "Imported.bazelproject",
      """
      directories:
        dirB

      try_import Main.bazelproject
      """.trimIndent(),
    )
    val psiFile =
      myFixture.configureByText(
        "Main.bazelproject",
        """
        directories:
          dirA

        try_import Imported.bazelproject
        """.trimIndent(),
      )
    val projectView = ProjectViewFactory.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(
      ExcludableValue.included(Path("dirA")),
      ExcludableValue.included(Path("dirB")),
    )
  }
}
