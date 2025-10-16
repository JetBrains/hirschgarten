package org.jetbrains.bazel.languages.projectview.documentation

import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkDocumentationTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewSectionDocumentationTest : StarlarkDocumentationTestCase() {
  @Test
  fun `should display documentation for section`() {
    myFixture.configureByText(".bazelproject", "tar<caret>gets: someTarget")
    val documentation = getDocumentationAtCaret()
    assertNotNull(documentation)
    documentation!! shouldContain "targets"
  }
}
