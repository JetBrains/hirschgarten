package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkBindingTest {
  private val myFixture by bazelCodeInsightFixture(testDataDir = TEST_DATA_DIR)

  @Test
  fun `names bind only in target positions`() {
    myFixture.configureByFile("Bindings.bzl")
    val dump = runReadActionBlocking {
      val text = myFixture.file.text
      myFixture.file.descendantsOfType<StarlarkReferenceExpression>()
        .mapNotNull { it.getIdentifier() }
        .sortedBy { it.textOffset }
        .joinToString("\n") { identifier ->
          val position = StringUtil.offsetToLineColumn(text, identifier.textOffset)
          val role = if (identifier.parent.isBinding()) "binding" else "reference"
          "${position.line + 1}:${position.column + 1} ${identifier.text} $role"
        }
    }
    PlatformTestUtil.assertSameLinesWithFile(BazelPathManager.getTestFixture(TEST_DATA_DIR) + "/Bindings.txt", dump)
  }
}

private const val TEST_DATA_DIR = "starlark/psi"
