package org.jetbrains.bazel.languages.starlark.indentation

import com.intellij.openapi.application.runReadActionBlocking
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkEnterBetweenBracesTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should add empty line after enter between braces`() {
    // given
    myFixture.configureByText("dummy.bzl", "[<caret>]")

    // when
    myFixture.type("\n")

    // then
    runReadActionBlocking { myFixture.editor.document.lineCount } shouldBe 3
  }
}
