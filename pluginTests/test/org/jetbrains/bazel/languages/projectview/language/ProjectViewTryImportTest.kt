package org.jetbrains.bazel.languages.projectview.language

import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.ModuleFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.sections.DirectoriesSection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.Path

/**
 * Tests for ProjectView import logic focusing on try_import and missing import cases.
 */
@RunWith(JUnit4::class)
class ProjectViewTryImportTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {
  @Test
  fun `test try_import missing file is ignored`() {
    // Ensure imports resolve from project root
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")

    val psiFile =
      myFixture.configureByText(
        "Main.bazelproject",
        """
        directories:
          dirB
        
        try_import Missing.bazelproject
        """.trimIndent(),
      )

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)

    projectView.getSection(DirectoriesSection.KEY)!! shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `test try_import present merges collections`() {
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)

    // Since try_import comes first, imported directories should appear before local ones
    projectView.getSection(DirectoriesSection.KEY)!! shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `test required import missing throws helpful error`() {
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")

    val psiFile =
      myFixture.configureByText(
        "Main.bazelproject",
        """
        import not_there.bazelproject
        """.trimIndent(),
      )

    val ex =
      shouldThrow<IllegalStateException> {
        ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
      }
    assertTrue(ex.toString().contains("Cannot find project view file requested in an import: not_there.bazelproject"))
  }
}
