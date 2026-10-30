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
class BazelNativeRuleArgumentDocumentationTargetTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should display documentation for rule argument`() {
    myFixture.configureByText("BUILD", "java_binary(na<caret>me = \"\")")

    val documentation = myFixture.documentationAtCaret().shouldNotBeNull()
    documentation shouldContain "name"
    documentation.length shouldBeGreaterThan "name".length
  }

  @Test
  fun `should not display documentation for an invalid rule argument`() {
    myFixture.configureByText("BUILD", "java_binary(not_an_<caret>arg = \"\")")

    myFixture.documentationAtCaret().shouldBeNull()
  }

  @Test
  fun `should not display documentation for non-rule arguments`() {
    myFixture.configureByText("BUILD", "not_a_rule(na<caret>me = \"\")")

    myFixture.documentationAtCaret().shouldBeNull()
  }
}
