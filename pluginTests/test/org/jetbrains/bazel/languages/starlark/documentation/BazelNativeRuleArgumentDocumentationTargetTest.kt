package org.jetbrains.bazel.languages.starlark.documentation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkDocumentationTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelNativeRuleArgumentDocumentationTargetTest : StarlarkDocumentationTestCase() {
  @Test
  fun `should display documentation for rule argument`() {
    myFixture.configureByText("BUILD", "java_binary(na<caret>me = \"\")")

    val documentation = getDocumentationAtCaret()
    assertNotNull(documentation)
    assertTrue(documentation!!.contains("name"))
    assertTrue(documentation.length > "name".length)
  }

  @Test
  fun `should not display documentation for an invalid rule argument`() {
    myFixture.configureByText("BUILD", "java_binary(not_an_<caret>arg = \"\")")

    val documentation = getDocumentationAtCaret()
    assertNull(documentation)
  }

  @Test
  fun `should not display documentation for non-rule arguments`() {
    myFixture.configureByText("BUILD", "not_a_rule(na<caret>me = \"\")")

    val documentation = getDocumentationAtCaret()
    assertNull(documentation)
  }
}
