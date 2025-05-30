package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiManager
import com.intellij.refactoring.RefactoringFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFileRenameTest : BasePlatformTestCase() {
  @Test
  fun `should update load files in BUILD`() {
    myFixture.addFileToProject("file_to_rename.bzl", "content")
    myFixture.addFileToProject(
      "BUILD",
      """
      load("//file_to_rename.bzl", "some_rule")
      """.trimIndent(),
    )

    doRename("file_to_rename.bzl", "renamed.bzl")

    val updatedFile = myFixture.findFileInTempDir("BUILD")
    val updatedText = PsiManager.getInstance(project).findFile(updatedFile!!)!!.text
    assertTrue(updatedText.contains("renamed.bzl"))
    assertFalse(updatedText.contains("file_to_rename.bzl"))
  }

  @Test
  fun `should update src files in BUILD`() {
    myFixture.addFileToProject("java_1.java", "java 1 content")
    myFixture.addFileToProject("java_2.java", "java 2 content")
    myFixture.addFileToProject("kotlin_1.kt", "kotlin 1 content")
    myFixture.addFileToProject("kotlin_2.kt", "kotlin 2 content")
    myFixture.addFileToProject(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
          name = "test_package",
          srcs = ["java_1.java", "java_2.java", "kotlin_1.kt", "kotlin_2.kt"],
      )
      """.trimIndent(),
    )

    doRename("java_2.java", "cpp_file.cpp")
    doRename("kotlin_1.kt", "another_kotlin.kt")

    val updatedFile = myFixture.findFileInTempDir("BUILD")
    val updatedText = PsiManager.getInstance(project).findFile(updatedFile!!)!!.text
    assertTrue(updatedText.contains(".bzl"))
    TestCase.assertEquals(
      updatedText,
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
          name = "test_package",
          srcs = ["java_1.java", "cpp_file.cpp", "another_kotlin.kt", "kotlin_2.kt"],
      )
      """.trimIndent(),
    )
  }

  private fun doRename(oldName: String, newName: String) {
    val project = myFixture.project
    val fileToRename = myFixture.findFileInTempDir(oldName) ?: error("File not found")
    val psiFile = PsiManager.getInstance(project).findFile(fileToRename) ?: error("PsiFile not found")

    WriteCommandAction.runWriteCommandAction(project) {
      val refactoring = RefactoringFactory.getInstance(project).createRename(psiFile, newName)
      refactoring.run()
    }
  }
}
