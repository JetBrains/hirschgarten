package org.jetbrains.bazel.languages.starlark.documentation

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.languages.starlark.fixtures.documentationAtCaret
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelGlobalFunctionDocumentationTargetTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should display documentation for build rules`() {
    myFixture.configureByText("BUILD", "java_<caret>binary()")

    val documentation = myFixture.documentationAtCaret().shouldNotBeNull()
    documentation shouldContain "java_binary"
    documentation.length shouldBeGreaterThan "java_binary".length
  }

  @Test
  fun `should display documentation for global functions in BUILD`() {
    myFixture.configureByText("BUILD", "existing_<caret>rule()")

    val documentation = myFixture.documentationAtCaret().shouldNotBeNull()
    documentation shouldContain "existing_rule"
    documentation.length shouldBeGreaterThan "existing_rule".length
  }

  @Test
  fun `should display documentation for global functions in MODULE bazel`() {
    myFixture.configureByText("MODULE.bazel", "use_<caret>repo()")

    val documentation = myFixture.documentationAtCaret().shouldNotBeNull()
    documentation shouldContain "use_repo"
    documentation.length shouldBeGreaterThan "use_repo".length
  }

  @Test
  fun `should not display documentation for non-rules`() {
    myFixture.configureByText("BUILD", "this_is_not<caret>_a_rule()")

    myFixture.documentationAtCaret().shouldBeNull()
  }
}
