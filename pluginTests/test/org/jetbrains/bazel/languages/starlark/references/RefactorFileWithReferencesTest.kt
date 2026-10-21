package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiElement
import com.intellij.testFramework.common.timeoutRunBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.workspace.setRepoMapping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

@BazelTestApplication
internal class RefactorFileWithReferencesTest {
  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project

  private val rootPath: Path
    get() = Path(myFixture.tempDirFixture.getFile(".")!!.path)

  @Test
  fun `rename kotlin file should update label in build file`() {
    val kotlinFile =
      myFixture.addFileToProject(
        "com/example/MyClass.kt",
        """
        package com.example
        class MyClass
        """.trimIndent(),
      )
    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
          name = "test_example",
          srcs = ["com/example/MyClass.kt"],
          main_class = "com.example.MyClass",
      )
      """.trimIndent(),
    )

    renameElement(kotlinFile, "RenamedClass.kt")

    myFixture.checkResult(
      """
      java_binary(
          name = "test_example",
          srcs = ["com/example/RenamedClass.kt"],
          main_class = "com.example.MyClass",
      )
      """.trimIndent(),
    )
  }

  @Test
  fun `rename java file should update label in build file`() {
    val javaFile =
      myFixture.addFileToProject(
        "com/example/MyClass.java",
        """
        package com.example;
        public class MyClass 
        """.trimIndent(),
      )
    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
          name = "test_example",
          srcs = ["com/example/MyClass.java"],
      )
      """.trimIndent(),
    )

    renameElement(javaFile, "RenamedClass.java")

    myFixture.checkResult(
      """
      java_binary(
          name = "test_example",
          srcs = ["com/example/RenamedClass.java"],
      )
      """.trimIndent(),
    )
  }

  @Test
  fun `rename bzl file should update label in build file`() {
    val starlarkFile =
      myFixture.addFileToProject(
        "java_rules_to_rename.bzl",
        """
        def custom_java_library(name, srcs = [], deps = []):
            native.java_library(name = name, srcs = srcs, deps = deps)
        """.trimIndent(),
      )
    myFixture.configureByText(
      "BUILD",
      """
      load(":java_rules_to_rename.bzl", "custom_java_library")
      
      custom_java_library(
          name = "my_lib",
          srcs = ["src/Example.java"],
      )
      """.trimIndent(),
    )

    renameElement(starlarkFile, "custom_java_rules.bzl")

    myFixture.checkResult(
      """
      load(":custom_java_rules.bzl", "custom_java_library")
      
      custom_java_library(
          name = "my_lib",
          srcs = ["src/Example.java"],
      )
      """.trimIndent(),
    )
  }

  @Test
  fun `rename file should not change glob pattern in build file`() {
    val kotlinFile =
      myFixture.addFileToProject(
        "TestUtils.kt",
        """
        package test
        object TestUtils 
        """.trimIndent(),
      )
    myFixture.configureByText(
      "BUILD",
      """
      kt_jvm_test(
          name = "test_example",
          srcs = glob(["*.kt"]),
          testonly = True,
      )
      """.trimIndent(),
    )

    renameElement(kotlinFile, "TestUtilsRenamed.kt")

    myFixture.checkResult(
      """
      kt_jvm_test(
          name = "test_example",
          srcs = glob(["*.kt"]),
          testonly = True,
      )
      """.trimIndent(),
    )
  }

  @Test
  fun `rename file should update labels in multiple build files`() {
    val sharedFile =
      myFixture.addFileToProject(
        "shared/Utils.java",
        """
        package shared;
        public class Utils
        """.trimIndent(),
      )
    val buildFile1 =
      myFixture.addFileToProject(
        "module1/BUILD",
        """
        java_library(
            name = "module1_lib",
            srcs = ["../shared/Utils.java"],
        )
        """.trimIndent(),
      )
    val buildFile2 =
      myFixture.addFileToProject(
        "module2/BUILD",
        """
        java_library(
            name = "module2_lib",
            srcs = ["../shared/Utils.java"],
        )
        """.trimIndent(),
      )

    renameElement(sharedFile, "UtilsRenamed.java")

    assertEquals(
      """
      java_library(
          name = "module1_lib",
          srcs = ["../shared/UtilsRenamed.java"],
      )
      """.trimIndent(),
      runReadActionBlocking { buildFile1.text },
    )
    assertEquals(
      """
      java_library(
          name = "module2_lib",
          srcs = ["../shared/UtilsRenamed.java"],
      )
      """.trimIndent(),
      runReadActionBlocking { buildFile2.text },
    )
  }

  @Test
  fun `rename file should not break unrelated references`() {
    val fileToRename =
      myFixture.addFileToProject(
        "module1/Utils.kt",
        """
        bazel
        package module1
        object Utils
        """.trimIndent(),
      )
    myFixture.addFileToProject(
      "module2/Utils.kt",
      """
      package module2
      object Utils  
      """.trimIndent(),
    )
    myFixture.configureByText(
      "BUILD",
      """
      kt_jvm_library(
          name = "lib1",
          srcs = ["module1/Utils.kt"],
      )
      kt_jvm_library(
          name = "lib2", 
          srcs = ["module2/Utils.kt"],
      )
      """.trimIndent(),
    )

    renameElement(fileToRename, "UtilsRenamed.kt")

    myFixture.checkResult(
      """
      kt_jvm_library(
          name = "lib1",
          srcs = ["module1/UtilsRenamed.kt"],
      )
      kt_jvm_library(
          name = "lib2", 
          srcs = ["module2/Utils.kt"],
      )
      """.trimIndent(),
    )
  }

  @Test
  fun `Move File on kotlin file should change label in BUILD file`() {
    project.setRepoMapping { mainRepo(rootPath) }

    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.addFileToProject(
      "com/example/A.kt",
      """
        package com.example
        class A
        """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/B.kt",
      """
        package com.example
        class B
        """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")
      kt_jvm_binary(
          name = "a",
          srcs = ["A.kt"],
          main_class = "com.example.A",
      )
      kt_jvm_binary(
          name = "b",
          srcs = ["B.kt"],
          main_class = "com.example.B",
          deps = ["//com/example:a"],
      )
      """.trimIndent(),
    )

    moveFile("com/example/A.kt", "com")

    myFixture.checkResult(
      "com/example/BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")
      kt_jvm_binary(
          name = "a",
          srcs = ["//:com/A.kt"],
          main_class = "com.example.A",
      )
      kt_jvm_binary(
          name = "b",
          srcs = ["B.kt"],
          main_class = "com.example.B",
          deps = ["//com/example:a"],
      )
      """.trimIndent(),
      true
    )
  }

  @Test
  fun `Move File on java file should change label in BUILD file`() {
    project.setRepoMapping { mainRepo(rootPath) }

    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.addFileToProject(
      "com/example/A.java",
      """
        package com.example;
        public class A {}
        """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/B.java",
      """
        package com.example;
        public class B {}
        """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/BUILD",
      """      
      java_binary(
          name = "a",
          srcs = ["A.java"],
          classname = "com.example.A",
      )
      java_binary(
          name = "b",
          srcs = ["B.java"],
          classname = "com.example.B",
          deps = ["//com/example:a"],
      )
      """.trimIndent(),
    )

    moveFile("com/example/A.java", "com")

    myFixture.checkResult(
      "com/example/BUILD",
      """      
      java_binary(
          name = "a",
          srcs = ["//:com/A.java"],
          classname = "com.example.A",
      )
      java_binary(
          name = "b",
          srcs = ["B.java"],
          classname = "com.example.B",
          deps = ["//com/example:a"],
      )
      """.trimIndent(),
      true
    )
  }

  @Test
  fun `Move File on bzl file should change label in BUILD file`() {
    project.setRepoMapping { mainRepo(rootPath) }

    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.addFileToProject(
      "com/example/bzl/custom.bzl",
      """
        def custom_java_library(name, srcs = [], deps = []):
            native.java_library(
                name = name,
                srcs = srcs,
                deps = deps,
            )
        """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/BUILD",
      """
      load("//com/example:bzl/custom.bzl", "custom_java_library")
      """.trimIndent(),
    )

    moveFile("com/example/bzl/custom.bzl", "com/example")

    myFixture.checkResult(
      "com/example/BUILD",
      """
      load("//com/example:custom.bzl", "custom_java_library")
      """.trimIndent(),
      true
    )
  }

  @Test
  fun `Move File to another bazel package should change label in BUILD file`() {
    project.setRepoMapping { mainRepo(rootPath) }

    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.addFileToProject(
      "com/example/custom.bzl",
      """
        def custom_java_library(name, srcs = [], deps = []):
            native.java_library(
                name = name,
                srcs = srcs,
                deps = deps,
            )
        """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/BUILD",
      """
      load("//com/example:custom.bzl", "custom_java_library")
      """.trimIndent(),
    )

    moveFile("com/example/custom.bzl", "com")

    myFixture.checkResult(
      "com/example/BUILD",
      """
      load("//:com/custom.bzl", "custom_java_library")
      """.trimIndent(),
      true
    )
  }


  @Test
  fun `Move File to another bazel repo change label in BUILD file`() {
    project.setRepoMapping {
      mainRepo(rootPath)
      repo("example", rootPath.resolve("com/example"))
    }

    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject(
      "BUILD",
      """
      load("//:custom.bzl", "custom_java_library")
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "custom.bzl",
      """
        def custom_java_library(name, srcs = [], deps = []):
            native.java_library(
                name = name,
                srcs = srcs,
                deps = deps,
            )
        """.trimIndent(),
    )
    myFixture.addFileToProject("com/example/MODULE.bazel", "module(name = 'example')")
    myFixture.addFileToProject("com/example/BUILD", "")

    moveFile("custom.bzl", "com/example")

    myFixture.checkResult(
      "BUILD",
      """
      load("@example//:custom.bzl", "custom_java_library")
      """.trimIndent(),
      true
    )
  }


  @Test
  fun `Move File should not change glob pattern in BUILD file`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.addFileToProject(
      "com/example/A.kt",
      """
        package com.example
        class A
        """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/BUILD",
      """
      kt_jvm_test(
          name = "test_example",
          srcs = glob(["*.kt"]),
          testonly = True,
      )
      """.trimIndent(),
    )

    moveFile("com/example/A.kt", "com")

    myFixture.checkResult(
      "com/example/BUILD",
      """
      kt_jvm_test(
          name = "test_example",
          srcs = glob(["*.kt"]),
          testonly = True,
      )
      """.trimIndent(),
      true,
    )
  }

  private fun renameElement(element: PsiElement, newName: String): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) { myFixture.renameElement(element, newName) }
  }

  private fun moveFile(filePath: String, to: String): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) { myFixture.moveFile(filePath, to) }
  }
}
