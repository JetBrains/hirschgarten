package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContainAnyOf
import org.jetbrains.bazel.config.isBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFileCompletionTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    project.isBazelProject = true
  }

  @Test
  fun `should complete in src`() {
    myFixture.addFileToProject("a.kt", "")
    myFixture.addFileToProject("b.kt", "")

    myFixture.configureByText(
      "BUILD",
      """
      load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")
      kt_intellij_junit4_test(
        name = "testTarget",
        src = <caret>
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"a.kt\"", "\"b.kt\"")
  }

  @Test
  fun `should complete in srcs`() {
    myFixture.addFileToProject("c.kt", "")
    myFixture.addFileToProject("d.kt", "")

    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
        name = "testTarget",
        srcs = [<caret>]
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"c.kt\"", "\"d.kt\"")
  }

  @Test
  fun `should complete in hdrs`() {
    myFixture.addFileToProject("folder1/e.kt", "")
    myFixture.addFileToProject("/folder2/f.kt", "")

    myFixture.configureByText(
      "BUILD",
      """
      cc_library(
        name = "testTarget",
        hdrs = [<caret>]
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"folder1/e.kt\"", "\"folder2/f.kt\"")
  }

  @Test
  fun `should complete in only one directory`() {
    myFixture.addFileToProject("testFile1.kt", "kotlin")
    myFixture.addFileToProject("testFile2.java", "java")
    myFixture.addFileToProject("testFile3.py", "python")
    myFixture.addFileToProject("testFile4.cpp", "c++")
    myFixture.addFileToProject("testFile5.rs", "rust")

    myFixture.configureByText(
      "BUILD",
      """
      load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")
      kt_intellij_junit4_test(
        name = "testTarget",
        src = <caret>
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder
      listOf(
        "\"testFile1.kt\"",
        "\"testFile2.java\"",
        "\"testFile3.py\"",
        "\"testFile4.cpp\"",
        "\"testFile5.rs\"",
      )
  }

  @Test
  fun `should complete in subdirectories but not in subpackages`() {
    /* Files structure:
    .
    ├── a.kt
    ├── b.kt
    ├── directory
    │   ├── d.py
    │   └── e.py
    └── package
          ├── BUILD
          ├── f.cpp
          ├── g.cpp
          └── dirInPackage
                  ├── h.java
                  └── i.java */
    myFixture.addFileToProject("a.kt", "")
    myFixture.addFileToProject("b.kt", "")
    myFixture.addFileToProject("directory/d.py", "")
    myFixture.addFileToProject("directory/e.py", "")
    myFixture.addFileToProject("package/f.cpp", "")
    myFixture.addFileToProject("package/g.cpp", "")
    myFixture.addFileToProject("package/BUILD", "")
    myFixture.addFileToProject("package/dirInPackage/h.java", "")
    myFixture.addFileToProject("package/dirInPackage/i.java", "")

    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
        name = "testTarget",
        srcs = [<caret>]
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder
      listOf(
        "\"a.kt\"",
        "\"b.kt\"",
        "\"directory/d.py\"",
        "\"directory/e.py\"",
      )
    lookups shouldNotContainAnyOf
      listOf(
        "\"package/f.cpp\"",
        "\"package/g.cpp\"",
        "\"package/dirInPackage/h.java\"",
        "\"package/dirInPackage/i.java\"",
      )
  }
}
