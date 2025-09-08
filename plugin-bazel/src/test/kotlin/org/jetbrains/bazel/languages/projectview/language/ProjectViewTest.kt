package org.jetbrains.bazel.languages.projectview

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.sections.TargetsSection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewTest : BasePlatformTestCase() {
  @Test
  fun `test project view from raw values`() {
    val rawSections =
      listOf(
        ProjectView.RawSection("targets", listOf("target1", "-target2")),
        ProjectView.RawSection("shard_sync", listOf("true")),
      )
    val projectView = ProjectView(rawSections, myFixture.project)

    val targetsSection = projectView.getSection(TargetsSection.KEY)
    targetsSection.shouldNotBeNull()
    targetsSection shouldContain ExcludableValue.included(Label.parse("target1"))
    targetsSection shouldContain ExcludableValue.excluded(Label.parse("target2"))

    val shardSyncSection = projectView.getSection(ShardSyncSection.KEY)
    shardSyncSection.shouldNotBeNull()
    assertEquals(shardSyncSection, true)
  }

  @Test
  fun `test project view from psi file`() {
    val file =
      myFixture.configureByText(
        ".bazelproject",
        """
        targets:
          target1
          -target2
        shard_sync: true
        """.trimIndent(),
      )
    val projectView = ProjectView.fromProjectViewPsiFile(file as ProjectViewPsiFile)

    val targetsSection = projectView.getSection(TargetsSection.KEY)
    targetsSection.shouldNotBeNull()
    targetsSection shouldContain ExcludableValue.included(Label.parse("target1"))
    targetsSection shouldContain ExcludableValue.excluded(Label.parse("target2"))

    val shardSyncSection = projectView.getSection(ShardSyncSection.KEY)
    shardSyncSection.shouldNotBeNull()
    assertEquals(shardSyncSection, true)
  }

  @Test
  fun `test list section serialization`() {
    val targetsSection =
      listOf(
        ExcludableValue.included(Label.parse("target1")),
        ExcludableValue.excluded(Label.parse("target2")),
      )
    val expected =
      """
      targets:
        target1
        -target2
      """.trimIndent()
    val targetsSectionModel = ProjectViewSections.getSectionByType<TargetsSection>()
    assertEquals(targetsSectionModel!!.serialize(targetsSection), expected)
  }

  @Test
  fun `test scalar section serialization`() {
    val shardSyncSection = true
    val expected = "shard_sync: true"
    val shardSyncSectionModel = ProjectViewSections.getSectionByType<ShardSyncSection>()
    assertEquals(shardSyncSectionModel!!.serialize(shardSyncSection), expected)
  }

  @Test
  fun `test variants annotation`() {
    val message = ProjectViewBundle.getMessage("annotator.unknown.variant.error") + " true, false"
    myFixture.configureByText(".bazelproject", """shard_sync: <error descr="$message">not_a_boolean</error>""")
    myFixture.checkHighlighting()
  }

  @Test
  fun `test unknown flag annotation`() {
    val message = ProjectViewBundle.getMessage("annotator.unknown.flag.error", "not_a_flag")
    myFixture.configureByText(".bazelproject", """build_flags: <warning descr="$message">not_a_flag</warning>""")
    myFixture.checkHighlighting()
  }

  @Test
  fun `test not applicable flag annotation`() {
    val message = ProjectViewBundle.getMessage("annotator.flag.not.allowed.here.error", "--dump_all", "build")
    myFixture.configureByText(".bazelproject", """build_flags: <warning descr="$message">--dump_all</warning>""")
    myFixture.checkHighlighting()
  }
}
