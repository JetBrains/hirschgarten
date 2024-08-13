package org.jetbrains.bazel.languages.starlark.indentation

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkEnterBetweenBracesTest : BasePlatformTestCase() {
  @Test
  fun `should add empty line after enter between braces`() {
    // given
    myFixture.configureByText("dummy.bzlmock", "[<caret>]")
    val expected = "[\n<caret>\n]"

    // when
    myFixture.type("\n")

    // then
    myFixture.checkResult(expected)
  }
}
