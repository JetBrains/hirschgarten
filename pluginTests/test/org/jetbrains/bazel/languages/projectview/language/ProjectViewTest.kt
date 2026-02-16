package org.jetbrains.bazel.languages.projectview.language

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewSections
import org.jetbrains.bazel.languages.projectview.buildFlags
import org.jetbrains.bazel.languages.projectview.debugFlags
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.sections.TargetsSection
import org.jetbrains.bazel.languages.projectview.syncFlags
import org.jetbrains.bazel.languages.projectview.testFlags
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewTest : BasePlatformTestCase() {
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
    val projectView = ProjectView.Companion.fromProjectViewPsiFile(file as ProjectViewPsiFile)

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
    val message = ProjectViewBundle.getMessage("annotator.flag.not.allowed.here.error", "--dump_all", "[build]")
    myFixture.configureByText(".bazelproject", """build_flags: <warning descr="$message">--dump_all</warning>""")
    myFixture.checkHighlighting()
  }

  @Test
  fun `test correct flag parsing`() {
    myFixture.configureByText(
      ".bazelproject",
      """
        sync_flags:
          --announce_rc
        build_flags: 
          --define=ij_product=intellij-latest
        debug_flags:
          --debugger_port=5555
        test_flags:
          --test_suite=MyTestSuite
        """.trimIndent(),
    )

    val pv = ProjectView.fromProjectViewPsiFile(myFixture.file as ProjectViewPsiFile)

    pv.syncFlags shouldContain "--announce_rc"
    pv.buildFlags shouldContain "--define=ij_product=intellij-latest"
    pv.debugFlags shouldContain "--debugger_port=5555"
    pv.testFlags shouldContain "--test_suite=MyTestSuite"
  }
}
