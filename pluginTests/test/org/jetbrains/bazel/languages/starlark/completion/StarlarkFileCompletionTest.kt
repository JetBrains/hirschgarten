package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.testFramework.fixtures.TestLookupElementPresentation
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkFileCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should show file icons in completion`() {
    val files = listOf("source.kt", "nested/defs.bzl").associateWith { myFixture.addFileToProject(it, "") }

    myFixture.configureByText(
      "BUILD",
      """
      filegroup(
        name = "testTarget",
        srcs = ["<caret>"]
      )
      """.trimIndent(),
    )
    val lookups = completeBasic().associateBy { it.lookupString }

    lookups.keys shouldContainExactlyInAnyOrder files.keys.map { "\"$it\"" } + "\"MODULE.bazel\""
    for ((path, file) in files) {
      val presentation = runReadActionBlocking { TestLookupElementPresentation.renderReal(lookups.getValue("\"$path\"")) }
      TestLookupElementPresentation.unwrapIcon(presentation.icon) shouldBe
        TestLookupElementPresentation.unwrapIcon(runReadActionBlocking { file.getIcon(0) })
    }
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
    val lookups = completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"a.kt\"", "\"b.kt\"", "\"MODULE.bazel\"")
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
    val lookups = completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"c.kt\"", "\"d.kt\"", "\"MODULE.bazel\"")
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
    val lookups = completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"folder1/e.kt\"", "\"folder2/f.kt\"", "\"MODULE.bazel\"")
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
    val lookups = completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder
      listOf(
        "\"testFile1.kt\"",
        "\"testFile2.java\"",
        "\"testFile3.py\"",
        "\"testFile4.cpp\"",
        "\"testFile5.rs\"",
        "\"MODULE.bazel\"",
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
    val lookups = completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder
      listOf(
        "\"a.kt\"",
        "\"b.kt\"",
        "\"directory/d.py\"",
        "\"directory/e.py\"",
        "\"MODULE.bazel\"",
      )
    lookups shouldNotContainAnyOf
      listOf(
        "\"package/f.cpp\"",
        "\"package/g.cpp\"",
        "\"package/dirInPackage/h.java\"",
        "\"package/dirInPackage/i.java\"",
      )
  }

  private fun completeBasic(): Array<LookupElement> = myFixture.completeBasic()
}
