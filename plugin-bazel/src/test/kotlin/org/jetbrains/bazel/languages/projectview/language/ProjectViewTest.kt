package org.jetbrains.bazel.languages.projectview.language

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewTest : TestCase() {
  @Test
  fun `test project view from raw values`() {
    val rawSections =
      listOf(
        "targets" to listOf("target1", "-target2"),
      )
    val projectView = ProjectView(rawSections)
    val targetsSection = projectView.getSection(TargetsSection.sectionKey)
    targetsSection.shouldNotBeNull()
    targetsSection.included shouldContain "target1"
    targetsSection.excluded shouldContain "target2"
  }
}
