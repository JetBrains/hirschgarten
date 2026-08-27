package org.jetbrains.bazel.languages.starlark.folding

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.jetbrains.bazel.test.framework.BazelPathManager

class StarlarkFoldingTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("starlark/folding")

  fun testFunction() = myFixture.testFolding(getTestDataPath() + "/function.bzl")

  fun testList() = myFixture.testFolding(getTestDataPath() + "/list.bzl")

  fun testNested() = myFixture.testFolding(getTestDataPath() + "/nested.bzl")

  fun testParens() = myFixture.testFolding(getTestDataPath() + "/parens.bzl")

  fun testTarget() = myFixture.testFolding(getTestDataPath() + "/target.bzl")

  /** A dict literal and a function body do not fold. */
  fun testDict() = myFixture.testFolding(getTestDataPath() + "/dict.bzl")

  /** A call on a dotted name shows the full name. */
  fun testQualifiedCall() = myFixture.testFolding(getTestDataPath() + "/qualifiedCall.bzl")

  /** A call on a subscription has no name, so the placeholder is the fallback. */
  fun testUnknownRule() = myFixture.testFolding(getTestDataPath() + "/unknownRule.bzl")

  fun testExpressionFoldsAreExpandedByDefault() {
    myFixture.configureByText(
      "expanded.bzl",
      """
      some_function(
          name = "target",
          arg1,
      )
      """.trimIndent(),
    )

    val description = (myFixture as CodeInsightTestFixtureImpl).getFoldingDescription(true, false)

    assertEquals(
      """
      <fold text='some_function(target)' expand='true'>some_function(
          name = "target",
          arg1,
      )</fold>
      """.trimIndent(),
      description,
    )
  }
}
