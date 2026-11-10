package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableElement
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
      myFixture.file.descendantsOfType<StarlarkLocalVariableElement>()
        .mapNotNull { it.getNameIdentifier() }
        .sortedBy { it.textOffset }
        .joinToString("\n") { identifier ->
          val position = StringUtil.offsetToLineColumn(text, identifier.textOffset)
          val role = if (identifier.parent is StarlarkTargetExpression) "binding" else "reference"
          "${position.line + 1}:${position.column + 1} ${identifier.text} $role"
        }
    }
    PlatformTestUtil.assertSameLinesWithFile(BazelPathManager.getTestFixture(TEST_DATA_DIR) + "/Bindings.txt", dump)
  }
}

private const val TEST_DATA_DIR = "starlark/psi"
