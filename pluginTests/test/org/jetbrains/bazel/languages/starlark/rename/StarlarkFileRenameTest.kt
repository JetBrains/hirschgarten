package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.config.isBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFileRenameTest : BasePlatformTestCase() {
  @Before
  fun setupBuildEnvironment() {
    project.isBazelProject = true
  }

  @Test
  fun `rename kotlin file should update usages in build file`() {
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

    myFixture.renameElement(kotlinFile, "RenamedClass.kt")

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
  fun `rename java file should update usages in build file`() {
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

    myFixture.renameElement(javaFile, "RenamedClass.java")

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
  fun `rename starlark file should update load statements in build file`() {
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

    myFixture.renameElement(starlarkFile, "custom_java_rules.bzl")

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

    myFixture.renameElement(kotlinFile, "TestUtilsRenamed.kt")

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
  fun `rename file should update usages in multiple build files`() {
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

    myFixture.renameElement(sharedFile, "UtilsRenamed.java")

    assertEquals(
      """
      java_library(
          name = "module1_lib",
          srcs = ["../shared/UtilsRenamed.java"],
      )
      """.trimIndent(),
      buildFile1.text,
    )
    assertEquals(
      """
      java_library(
          name = "module2_lib",
          srcs = ["../shared/UtilsRenamed.java"],
      )
      """.trimIndent(),
      buildFile2.text,
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

    myFixture.renameElement(fileToRename, "UtilsRenamed.kt")

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
}
