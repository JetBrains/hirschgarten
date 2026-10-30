package org.jetbrains.bazel.languages.starlark.folding

import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkFoldingTest {
  private val myFixture by bazelCodeInsightFixture()

  @BeforeEach
  fun beforeEach() {
    myFixture.testDataPath = BazelPathManager.getTestFixture("starlark/folding")
  }

  @Test
  fun testFunction() = doTest("function.bzl")

  @Test
  fun testList() = doTest("list.bzl")

  @Test
  fun testNested() = doTest("nested.bzl")

  @Test
  fun testParens() = doTest("parens.bzl")

  @Test
  fun testTarget() = doTest("target.bzl")

  /** A dict literal and a function body do not fold. */
  @Test
  fun testDict() = doTest("dict.bzl")

  /** A call on a dotted name shows the full name. */
  @Test
  fun testQualifiedCall() = doTest("qualifiedCall.bzl")

  /** A call on a subscription has no name, so the placeholder is the fallback. */
  @Test
  fun testUnknownRule() = doTest("unknownRule.bzl")

  @Test
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

    description shouldBe
      """
      <fold text='some_function(target)' expand='true'>some_function(
          name = "target",
          arg1,
      )</fold>
      """.trimIndent()
  }

  @Test
  fun testRegion() = doTest("region.bzl")

  @Test
  fun testRegionNoDescription() = doTest("regionNoDescription.bzl")

  @Test
  fun testRegionNested() = doTest("regionNested.bzl")

  @Test
  fun testRegionInFunction() = doTest("regionInFunction.bzl")

  /** An unpaired marker makes no region. */
  @Test
  fun testRegionUnmatched() = doTest("regionUnmatched.bzl")

  @Test
  fun testRegionAroundCall() = doTest("regionAroundCall.bzl")

  /** Extra hash characters before the marker still make a region. */
  @Test
  fun testRegionExtraHashes() = doTest("regionExtraHashes.bzl")

  /** The NetBeans style markers also make a region. */
  @Test
  fun testEditorFold() = doTest("editorFold.bzl")

  private fun doTest(fileName: String) {
    myFixture.testFolding("${myFixture.testDataPath}/$fileName")
  }
}
