package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.refactoring.rename.RenameUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.psi.requireCallWithNameAttribute
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelTargetRenameInputValidatorTest : BasePlatformTestCase() {

  @Before
  fun setupBuildEnvironment() {
    BazelProjectFixtures.initializeBazelProject(project, myFixture.tempDirPath)
  }

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
    val element = myFixture.requireCallWithNameAttribute("my_lib")
    assertEquals(expected, RenameUtil.isValidName(project, element, newName))
  }
}
