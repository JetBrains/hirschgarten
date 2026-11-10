package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkHighlightUsagesTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should highlight all bindings and reads from a read`() {
    highlightedOffsets(
      """
      def f():
          x = 1
          x = 2
          return <caret>x
      """.trimIndent(),
    ) shouldBe offsetsOf("x = 1", "x = 2", "return x")
  }

  @Test
  fun `should highlight all bindings and reads from a reassignment`() {
    highlightedOffsets(
      """
      def f():
          x = 1
          <caret>x = 2
          return x
      """.trimIndent(),
    ) shouldBe offsetsOf("x = 1", "x = 2", "return x")
  }

  @Test
  fun `should not highlight a qualified name with the same name`() {
    highlightedOffsets(
      """
      def f():
          x = 1
          foo = struct()
          foo.x = 2
          return <caret>x
      """.trimIndent(),
    ) shouldBe offsetsOf("x = 1", "return x")
  }

  private fun highlightedOffsets(text: String): List<Int> = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      myFixture.configureByText("defs.bzl", text)
      HighlightUsagesHandler.invoke(myFixture.project, myFixture.editor, myFixture.file)
      myFixture.editor.markupModel.allHighlighters.map { it.startOffset }.sorted()
    }
  }

  private fun offsetsOf(vararg fragments: String): List<Int> = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val text = myFixture.editor.document.text
      fragments.map { text.indexOf(it) + it.indexOf("x") }
    }
  }
}
