package org.jetbrains.bazel.languages.starlark.references

import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkMethodInvocationTest {
  private val myFixture by bazelCodeInsightFixture()

  // https://youtrack.jetbrains.com/issue/BAZEL-3039
  @Test
  fun `local function invocation`() {
    myFixture.verifyTargetOfReferenceAtCaret(
      """
        def <target>_compiler_target_actual(actual):
           return "@rules_kotlin_maven//:%s" % actual

        def kt_define_compiler_targets():
           for name, actual in _COMPILER_TARGETS:
               native.alias(
                   name = name,
                   actual = <caret>_compiler_target_actual(actual),
               )
      """.trimIndent(),
    )
  }
}
