package org.jetbrains.bazel.languages.projectview.language

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import junit.framework.TestCase
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewTest : BasePlatformTestCase() {
  @Test
  fun `test project view from raw values`() {
    val rawSections =
      listOf(
        "targets" to listOf("target1", "-target2"),
        "shard_sync" to listOf("true"),
      )
    val projectView = ProjectView(rawSections)

    val targetsSection = projectView.getSection(TargetsSection.sectionKey)
    targetsSection.shouldNotBeNull()
    targetsSection.included shouldContain "target1"
    targetsSection.excluded shouldContain "target2"

    val shardSyncSection = projectView.getSection(ShardSyncSection.sectionKey)
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

    val targetsSection = projectView.getSection(TargetsSection.sectionKey)
    targetsSection.shouldNotBeNull()
    targetsSection.included shouldContain "target1"
    targetsSection.excluded shouldContain "target2"

    val shardSyncSection = projectView.getSection(ShardSyncSection.sectionKey)
    shardSyncSection.shouldNotBeNull()
    assertEquals(shardSyncSection, true)
  }
}
