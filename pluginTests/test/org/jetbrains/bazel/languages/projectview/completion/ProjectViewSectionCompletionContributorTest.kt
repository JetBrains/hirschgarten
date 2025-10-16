package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAll
import org.jetbrains.bazel.languages.projectview.ProjectViewSections
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewSectionCompletionContributorTest : BasePlatformTestCase() {
  @Test
  fun `should suggest section names in top level`() {
    myFixture.configureByText(".bazelproject", "")

    myFixture.type("a")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    val expected = ProjectViewSections.REGISTERED_SECTIONS.filter { it.name.contains("a") }.map { it.name }
    lookups shouldContainAll expected
  }

  @Test
  fun `should not suggest anything if not top level`() {
    myFixture.configureByText(".bazelproject", "targets: <caret>")

    myFixture.type("a")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    lookups shouldNotContainAll ProjectViewSections.REGISTERED_SECTIONS.filter { it.name.contains("a") }.map { it.name }
  }

  @Test
  fun `should not suggest section names inside sections`() {
    myFixture.configureByText(
      ".bazelproject",
      """
      targets:
        //some/target
        <caret>
      """.trimIndent(),
    )

    myFixture.type("a")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    lookups shouldNotContainAll ProjectViewSections.REGISTERED_SECTIONS.filter { it.name.contains("a") }.map { it.name }
  }
}
