package org.jetbrains.bazel.languages.projectview.documentation

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.languages.starlark.fixtures.documentationAtCaret
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class ProjectViewSectionDocumentationTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should display documentation for section`() {
    myFixture.configureByText(".bazelproject", "tar<caret>gets: someTarget")
    val documentation = myFixture.documentationAtCaret()
    documentation.shouldNotBeNull()
    documentation shouldContain "targets"
  }
}
