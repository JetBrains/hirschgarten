package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.config.isBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkStringLiteralInsertingTest : BasePlatformTestCase() {
  @Before
  fun setupBuildEnvironment() {
    project.isBazelProject = true
    myFixture.addFileToProject(
      "MyTest.kt",
      """
      package com.example
      class MyTest
      """.trimIndent(),
    )
  }

  @Test
  fun `should insert inside quotes`() {
    myFixture.configureByText(
      "BUILD",
      """
      kt_intellij_junit4_test(
        src = "My<caret>",
        name = "MyTest",
    )
      """.trimMargin(),
    )
    myFixture.completeBasic()
    myFixture.type('\n')

    myFixture.checkResult(
      """
      kt_intellij_junit4_test(
        src = "MyTest.kt"<caret>,
        name = "MyTest",
    )
      """.trimMargin(),
    )
  }

  @Test
  fun `should insert replacing full string inside quotes`() {
    myFixture.configureByText(
      "BUILD",
      """
      kt_intellij_junit4_test(
        src = "My<caret>.kt",
        name = "MyTest",
    )
      """.trimMargin(),
    )
    myFixture.completeBasic()
    myFixture.type('\n')

    myFixture.checkResult(
      """
      kt_intellij_junit4_test(
        src = "MyTest.kt"<caret>,
        name = "MyTest",
    )
      """.trimMargin(),
    )
  }

  @Test
  fun `should insert replacing string without ending quote`() {
    myFixture.configureByText(
      "BUILD",
      """
      kt_intellij_junit4_test(
        src = "My<caret>.kt,
        name = "MyTest",
    )
      """.trimMargin(),
    )
    myFixture.completeBasic()
    myFixture.type('\n')

    myFixture.checkResult(
      """
      kt_intellij_junit4_test(
        src = "MyTest.kt"<caret>,
        name = "MyTest",
    )
      """.trimMargin(),
    )
  }

  @Test
  fun `should insert replacing string without ending quote and comma`() {
    myFixture.configureByText(
      "BUILD",
      """
      kt_intellij_junit4_test(
        src = "My<caret>.kt
        name = "MyTest",
    )
      """.trimMargin(),
    )
    myFixture.completeBasic()
    myFixture.type('\n')

    myFixture.checkResult(
      """
      kt_intellij_junit4_test(
        src = "MyTest.kt"<caret>
        name = "MyTest",
    )
      """.trimMargin(),
    )
  }

  @Test
  fun `should insert replacing string at the end of file`() {
    myFixture.configureByText(
      "BUILD",
      """
      kt_intellij_junit4_test(
        src = "My<caret>.kt
      """.trimMargin(),
    )
    myFixture.completeBasic()
    myFixture.type('\n')

    myFixture.checkResult(
      """
      kt_intellij_junit4_test(
        src = "MyTest.kt"<caret>
      """.trimMargin(),
    )
  }
}
