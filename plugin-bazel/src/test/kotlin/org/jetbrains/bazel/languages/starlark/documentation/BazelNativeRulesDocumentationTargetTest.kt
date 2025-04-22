package org.jetbrains.bazel.languages.starlark.documentation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkDocumentationTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelNativeRulesDocumentationTargetTest : StarlarkDocumentationTestCase() {
  @Test
  fun `should display stardoc-generated documentation`() {
    myFixture.configureByText("BUILD", "java_<caret>binary()")

    val documentation = getDocumentationAtCaret()
    assertNotNull(documentation)
    assertTrue(documentation!!.contains("java_binary"))
    assertTrue(documentation!!.length > "java_binary".length)
  }

  @Test
  fun `should display link to external documentation when missing docString`() {
    myFixture.configureByText("BUILD", "cc_<caret>binary()")

    val documentation = getDocumentationAtCaret()
    assertNotNull(documentation)
    assertTrue(documentation!!.contains("External documentation for "))
  }

  @Test
  fun `should not display documentation for non-rules`() {
    myFixture.configureByText("BUILD", "this_is_not<caret>_a_rule()")

    val documentation = getDocumentationAtCaret()
    assertNull(documentation)
  }
}
