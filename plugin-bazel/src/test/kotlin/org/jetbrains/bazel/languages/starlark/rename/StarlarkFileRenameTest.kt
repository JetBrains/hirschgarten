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
    myFixture.addFileToProject("src/java/java_1.java", "java 1 content")
    myFixture.addFileToProject("src/java/java_2.java", "java 2 content")
    myFixture.addFileToProject("src/kotlin/kotlin_1.kt", "kotlin 1 content")
    myFixture.addFileToProject("src/kotlin/kotlin_2.kt", "kotlin 2 content")
    myFixture.addFileToProject(
      "src/BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
          name = "test_package",
          srcs = ["java/java_1.java", "java/java_2.java", "kotlin/kotlin_1.kt", "kotlin/kotlin_2.kt"],
      )
      """.trimIndent(),
    )

    doRename("src/java/java_2.java", "cpp_file.cpp")
    doRename("src/kotlin/kotlin_1.kt", "another_kotlin.kt")

    val updatedFile = myFixture.findFileInTempDir("src/BUILD")
    val updatedText = PsiManager.getInstance(project).findFile(updatedFile!!)!!.text
    assertTrue(updatedText.contains(".bzl"))
    TestCase.assertEquals(
      updatedText,
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
          name = "test_package",
          srcs = ["java/java_1.java", "java/cpp_file.cpp", "kotlin/another_kotlin.kt", "kotlin/kotlin_2.kt"],
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
