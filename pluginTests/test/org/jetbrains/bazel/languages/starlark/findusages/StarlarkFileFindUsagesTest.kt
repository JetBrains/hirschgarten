package org.jetbrains.bazel.languages.starlark.findusages

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkFindUsagesTestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFileFindUsagesTest : StarlarkFindUsagesTestCase() {
  @Before
  fun setupBuildEnvironment() {
    project.isBazelProject = true
  }

  @Test
  fun `should find usage for java file`() {
    val javaFile =
      myFixture.addFileToProject(
        "com/example/MyClass.java",
        """
        package com.example;
        public class MyClass {}
        """.trimIndent(),
      )

    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
          name = "test_example",
          srcs = ["com/example/MyClass.java"]
      )
      """.trimIndent(),
    )

    val usages = myFixture.findUsages(javaFile).mapNotNull { it.element?.text }
    usages.shouldContain(
      """
      "com/example/MyClass.java"
      """.trimIndent()
    )
  }

  @Test
  fun `should find usage for kotlin file`() {
    val ktFile =
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
          srcs = ["com/example/MyClass.kt"]
      )
      """.trimIndent(),
    )

    val usages = myFixture.findUsages(ktFile).mapNotNull { it.element?.text }
    usages.shouldContain(
      """
      "com/example/MyClass.kt"
      """.trimIndent()
    )
  }

  @Test
  fun `should find usage for file in matching glob pattern`() {
    val ktFile =
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
          srcs = glob(["**/*.kt"]),
          main_class = "com.example.MyClass",
      )
      """.trimIndent(),
    )

    val usages = myFixture.findUsages(ktFile).mapNotNull { it.element?.text }
    usages.shouldContain(
      """
      glob(["**/*.kt"])
      """.trimIndent()
    )
  }

  @Test
  fun `should not find usage for file in unmatching glob pattern`() {
    val ktFile =
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
          srcs = glob(["**/*.java"]),
          main_class = "com.example.MyClass",
      )
      """.trimIndent(),
    )

    val usages = myFixture.findUsages(ktFile)
    usages.shouldBeEmpty()
  }

  @Test
  fun `should not find usage for file in glob if excluded`() {
    val ktFile =
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
          srcs = glob(includes = ["**/*"], excludes = ["**/*.kt"]),
          main_class = "com.example.MyClass",
      )
      """.trimIndent(),
    )

    val usages = myFixture.findUsages(ktFile)
    usages.shouldBeEmpty()
  }
}
