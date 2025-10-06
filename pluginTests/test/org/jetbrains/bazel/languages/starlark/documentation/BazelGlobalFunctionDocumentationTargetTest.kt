package org.jetbrains.bazel.languages.starlark.documentation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkDocumentationTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelGlobalFunctionDocumentationTargetTest : StarlarkDocumentationTestCase() {
  @Test
  fun `should display documentation for build rules`() {
    myFixture.configureByText("BUILD", "java_<caret>binary()")

    val documentation = getDocumentationAtCaret()!!
    assertTrue(documentation.contains("java_binary"))
    assertTrue(documentation.length > "java_binary".length)
  }

  @Test
  fun `should display documentation for global functions in BUILD`() {
    myFixture.configureByText("BUILD", "existing_<caret>rule()")

    val documentation = getDocumentationAtCaret()!!
    assertTrue(documentation.contains("existing_rule"))
    assertTrue(documentation.length > "existing_rule".length)
  }

  @Test
  fun `should display documentation for global functions in MODULE bazel`() {
    myFixture.configureByText("MODULE.bazel", "use_<caret>repo()")

    val documentation = getDocumentationAtCaret()!!
    assertTrue(documentation.contains("use_repo"))
    assertTrue(documentation.length > "use_repo".length)
  }

  @Test
  fun `should not display documentation for non-rules`() {
    myFixture.configureByText("BUILD", "this_is_not<caret>_a_rule()")

    val documentation = getDocumentationAtCaret()
    assertNull(documentation)
  }
}
