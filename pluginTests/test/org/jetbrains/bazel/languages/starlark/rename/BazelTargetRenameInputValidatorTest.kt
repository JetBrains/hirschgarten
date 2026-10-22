package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.refactoring.rename.RenameUtil
import org.jetbrains.bazel.languages.starlark.psi.requireCallWithNameAttribute
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelTargetRenameInputValidatorTest {
  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project

  @Test
  fun `valid simple name`() {
    assertTargetNameValid("my_lib", expected = true)
  }

  @Test
  fun `valid name with dots`() {
    assertTargetNameValid("lib.jar", expected = true)
  }

  @Test
  fun `valid name with hyphens`() {
    assertTargetNameValid("my-lib", expected = true)
  }

  @Test
  fun `valid name with special chars`() {
    assertTargetNameValid("lib+extra", expected = true)
  }

  @Test
  fun `valid name with slash`() {
    assertTargetNameValid("some/target", expected = true)
  }

  @Test
  fun `invalid empty name`() {
    assertTargetNameValid("", expected = false)
  }

  @Test
  fun `invalid name with spaces`() {
    assertTargetNameValid("my lib", expected = false)
  }

  @Test
  fun `invalid name with colon`() {
    assertTargetNameValid("name:colon", expected = false)
  }

  @Test
  fun `invalid name with backtick`() {
    assertTargetNameValid("name`tick", expected = false)
  }

  private fun assertTargetNameValid(newName: String, expected: Boolean) {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "my_lib",
      )
      """.trimIndent(),
    )
    val isValid = runReadActionBlocking {
      val element = myFixture.requireCallWithNameAttribute("my_lib")
      RenameUtil.isValidName(project, element, newName)
    }
    assertEquals(expected, isValid)
  }
}
