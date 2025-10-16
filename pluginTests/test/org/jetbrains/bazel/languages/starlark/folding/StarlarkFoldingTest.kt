package org.jetbrains.bazel.languages.starlark.folding

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager

class StarlarkFoldingTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("starlark/folding")

  fun testFunction() = myFixture.testFolding(getTestDataPath() + "/function.bzl")

  fun testList() = myFixture.testFolding(getTestDataPath() + "/list.bzl")

  fun testNested() = myFixture.testFolding(getTestDataPath() + "/nested.bzl")

  fun testParens() = myFixture.testFolding(getTestDataPath() + "/parens.bzl")

  fun testTarget() = myFixture.testFolding(getTestDataPath() + "/target.bzl")
}
