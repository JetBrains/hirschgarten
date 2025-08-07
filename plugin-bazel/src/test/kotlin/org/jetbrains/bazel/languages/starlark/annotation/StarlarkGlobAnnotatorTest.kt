package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkGlobAnnotatorTest : StarlarkAnnotatorTestCase() {
  @Before
  fun beforeEach() {
    project.isBazelProject = true
  }

  @Test
  fun testNoArgsGlobAnnotator() {
    myFixture.configureByFile("NoArgsGlobAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyIncludesGlobAnnotator() {
    myFixture.configureByFile("EmptyIncludesGlobAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyUnnamedIncludesGlobAnnotator() {
    myFixture.configureByFile("EmptyUnnamedIncludesGlobAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyGlobAnnotator() {
    myFixture.addFileToProject("example1.java", "")
    myFixture.addFileToProject("example2.java", "")
    myFixture.configureByFile("EmptyGlobAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testUnresolvedGlobPatternsAnnotator() {
    myFixture.configureByFile("UnresolvedGlobPatternsAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testSomeUnresolvedGlobPatternsAnnotator() {
    myFixture.addFileToProject("example1.java", "")
    myFixture.addFileToProject("example2.kt", "")
    myFixture.configureByFile("SomeUnresolvedGlobPatternsAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyGlobAllowedAnnotator() {
    myFixture.configureByFile("EmptyGlobAllowedAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testUnresolvedGlobPatternsAllowedAnnotator() {
    myFixture.configureByFile("UnresolvedGlobPatternsAllowedAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }
}
