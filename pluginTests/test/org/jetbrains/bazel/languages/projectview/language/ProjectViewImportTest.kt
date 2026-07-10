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
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.sections.DirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.ImportDepthSection
import org.jetbrains.bazel.languages.projectview.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.sections.TargetsSection
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.test.framework.annotation.BazelTest
import org.junit.Test
import kotlin.io.path.Path

//@RunWith(JUnit4::class)
@BazelTest
class ProjectViewImportTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {
  override fun setUp() {
    super.setUp()
    initializeBazelProject(project, myFixture.tempDirPath)
  }

  @Test
  fun `test import no overlap`() {
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
    assertEquals(projectView.getSection(ImportDepthSection.KEY), 123)
    assertEquals(projectView.getSection(ShardSyncSection.KEY), false)
    projectView.getSection(TargetsSection.KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Label.parse("targetA")),
        ExcludableValue.included(Label.parse("targetB")),
      )
    projectView.getSection(DirectoriesSection.KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `test import full overlap`() {
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
    assertEquals(projectView.getSection(ImportDepthSection.KEY), 123)
    projectView.getSection(TargetsSection.KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Label.parse("targetC")),
        ExcludableValue.included(Label.parse("targetD")),
        ExcludableValue.included(Label.parse("targetA")),
        ExcludableValue.included(Label.parse("targetB")),
      )
  }

  @Test
  fun `test import full overlap import first`() {
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
    assertEquals(projectView.getSection(ImportDepthSection.KEY), 321)
    projectView.getSection(TargetsSection.KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Label.parse("targetA")),
        ExcludableValue.included(Label.parse("targetB")),
        ExcludableValue.included(Label.parse("targetC")),
        ExcludableValue.included(Label.parse("targetD")),
      )
  }

  @Test
  fun `test resolved import mapping`() {
    val importedFile = myFixture.addFileToProject("A.bazelproject", "import_depth: 1")
    val psiFile = myFixture.configureByText("B.bazelproject", "import A.bazelproject")
    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    projectView.imports.shouldBeSingleton {
      val resolved = it.shouldBeInstanceOf<Import.Resolved>()
      resolved.file shouldBe importedFile.virtualFile
      resolved.isRequired shouldBe true
    }
  }

  @Test
  fun `test unresolved import mapping`() {
    val psiFile =
      myFixture.configureByText(
        "B.bazelproject",
        """
        directories:
          dirA

        import missing.bazelproject
        """.trimIndent(),
      )
    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
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
    val psiFile =
      myFixture.configureByText(
        "B.bazelproject",
        """
        import_depth: 42
        directories:
          dirA
          dirB

        import missing.bazelproject
        """.trimIndent(),
      )
    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    projectView.imports.shouldBeSingleton {
      it.shouldBeInstanceOf<Import.Unresolved>()
    }
    projectView.getSection(ImportDepthSection.KEY) shouldBe 42
    projectView.getSection(DirectoriesSection.KEY) shouldContainExactly
      listOf(
        ExcludableValue.included(Path("dirA")),
        ExcludableValue.included(Path("dirB")),
      )
  }

  @Test
  fun `test self import does not recurse infinitely nor duplicate sections`() {
    val psiFile = myFixture.addFileToProject(
      "A.bazelproject",
      """
        targets:
          targetA

        import A.bazelproject
        """.trimIndent(),
    )
    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    projectView.getSection(TargetsSection.KEY) shouldContainExactly listOf(ExcludableValue.included(Label.parse("targetA")))
    projectView.imports.shouldBeSingleton {
      it.shouldBeInstanceOf<Import.Resolved>()
    }
  }

  @Test
  fun `test import cycle back to root terminates without duplicating sections`() {
    myFixture.addFileToProject(
      "B.bazelproject",
      """
      targets:
        targetB

      import A.bazelproject
      """.trimIndent(),
    )
    val psiFile = myFixture.configureByText(
      "A.bazelproject",
      """
        targets:
          targetA

        import B.bazelproject
        """.trimIndent(),
    )
    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    projectView.getSection(TargetsSection.KEY) shouldContainExactly listOf(
      ExcludableValue.included(Label.parse("targetA")),
      ExcludableValue.included(Label.parse("targetB")),
    )
  }
}
