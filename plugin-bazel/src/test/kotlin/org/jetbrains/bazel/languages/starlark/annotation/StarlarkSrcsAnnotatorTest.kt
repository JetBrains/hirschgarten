package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.testFramework.ExtensionTestUtil
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.bazel.StarlarkFilesListParametersProvider
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkSrcsAnnotatorTest : StarlarkAnnotatorTestCase() {
  @Before
  fun beforeEach() {
    project.isBazelProject = true
    val provider =
      object : StarlarkFilesListParametersProvider {
        override fun getFilesListParameters(): List<String> = listOf("my_srcs")
      }
    ExtensionTestUtil.maskExtensions(StarlarkFilesListParametersProvider.EP_NAME, listOf(provider), testRootDisposable)
  }

  @Test
  fun testUnresolvedSrcsAnnotator() {
    myFixture.configureByFile("UnresolvedSrcsAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testMultipleUnresolvedSrcsAnnotator() {
    myFixture.configureByFile("MultipleUnresolvedSrcsAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testSomeUnresolvedSrcsAnnotator() {
    myFixture.addFileToProject("Resolved.java", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.configureByFile("SomeUnresolvedSrcsAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testSomeUnresolvedKtSrcsAnnotator() {
    myFixture.addFileToProject("Resolved.kt", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.configureByFile("SomeUnresolvedKtSrcsAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyGlobAnnotator() {
    myFixture.configureByFile("EmptyGlobAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testUnresolvedGlobAnnotator() {
    myFixture.configureByFile("UnresolvedGlobAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }
}
