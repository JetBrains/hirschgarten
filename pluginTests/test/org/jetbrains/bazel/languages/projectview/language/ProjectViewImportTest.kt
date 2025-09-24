package org.jetbrains.bazel.languages.projectview.language

import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.ModuleFixture
import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.sections.DirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.ImportDepthSection
import org.jetbrains.bazel.languages.projectview.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.sections.TargetsSection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.Path

@RunWith(JUnit4::class)
class ProjectViewImportTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {
  @Test
  fun `test import no overlap`() {
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")

    myFixture.addFileToProject(
      "A.bazelproject",
      """
      import_depth: 123
      targets:
        targetA
        targetB
      """.trimIndent(),
    )
    val psiFile =
      myFixture.configureByText(
        "B.bazelproject",
        """
        shard_sync: false
        directories:
          dirA
          dirB
          
        import A.bazelproject
        """.trimIndent(),
      )
    val projectView = ProjectView.Companion.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    assertEquals(projectView.sections[ImportDepthSection.KEY], 123)
    assertEquals(projectView.sections[ShardSyncSection.KEY], false)
    projectView.getSection(TargetsSection.KEY)!! shouldContainExactly
      listOf(
        ExcludableValue.included(Label.parse("targetA")),
        ExcludableValue.included(Label.parse("targetB")),
      )
    projectView.getSection(DirectoriesSection.KEY)!! shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `test import full overlap`() {
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")

    myFixture.addFileToProject(
      "A.bazelproject",
      """
      import_depth: 123
      targets:
        targetA
        targetB
      """.trimIndent(),
    )
    val psiFile =
      myFixture.configureByText(
        "B.bazelproject",
        """
        import_depth: 321
        targets:
          targetC
          targetD
          
        import A.bazelproject
        """.trimIndent(),
      )
    val projectView = ProjectView.Companion.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    assertEquals(projectView.sections[ImportDepthSection.KEY], 123)
    projectView.getSection(TargetsSection.KEY)!! shouldContainExactly
      listOf(
        ExcludableValue.included(Label.parse("targetC")),
        ExcludableValue.included(Label.parse("targetD")),
        ExcludableValue.included(Label.parse("targetA")),
        ExcludableValue.included(Label.parse("targetB")),
      )
  }

  @Test
  fun `test import full overlap import first`() {
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")

    myFixture.addFileToProject(
      "A.bazelproject",
      """
      import_depth: 123
      targets:
        targetA
        targetB
      """.trimIndent(),
    )
    val psiFile =
      myFixture.configureByText(
        "B.bazelproject",
        """
        import A.bazelproject
        
        import_depth: 321
        targets:
          targetC
          targetD
        """.trimIndent(),
      )
    val projectView = ProjectView.Companion.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    assertEquals(projectView.sections[ImportDepthSection.KEY], 321)
    projectView.getSection(TargetsSection.KEY)!! shouldContainExactly
      listOf(
        ExcludableValue.included(Label.parse("targetA")),
        ExcludableValue.included(Label.parse("targetB")),
        ExcludableValue.included(Label.parse("targetC")),
        ExcludableValue.included(Label.parse("targetD")),
      )
  }
}
