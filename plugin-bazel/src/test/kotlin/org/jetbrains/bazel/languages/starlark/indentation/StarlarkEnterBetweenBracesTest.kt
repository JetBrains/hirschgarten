package org.jetbrains.bazel.languages.starlark.indentation

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkEnterBetweenBracesTest : BasePlatformTestCase() {
  @Test
  fun `should add empty line after enter between braces`() {
    // given
    myFixture.configureByText("dummy.bzl", "[<caret>]")

    // when
    myFixture.type("\n")

    // then
    myFixture.editor.document.lineCount shouldBeEqual 3
  }
}
