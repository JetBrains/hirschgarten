package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.config.isBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkClassCompletionTest : BasePlatformTestCase() {
  @Before
  fun setupBuildEnvironment() {
    project.isBazelProject = true
  }

  @Test
  fun `should suggest classes for classname attribute`() {
    // given
    myFixture.addFileToProject(
      "com/example/MyClass.java",
      """
      package com.example;
      public class MyClass {}
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/MyOtherClass.java",
      """
      package com.example;
      public class MyOtherClass {}
      """.trimIndent(),
    )

    // when
    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "test_example",
        classname = "com.example.<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("\"com.example.MyClass\"", "\"com.example.MyOtherClass\"")
  }

  @Test
  fun `should suggest subpackage for classname attribute`() {
    // given
    myFixture.addFileToProject(
      "com/example/MyClass.java",
      """
      package com.example;
      public class MyClass {}
      """.trimIndent(),
    )

    myFixture.addFileToProject(
      "com/example2/MyOtherClass.java",
      """
      package com.example;
      public class MyOtherClass {}
      """.trimIndent(),
    )

    // when
    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "test_example",
        classname = "com.<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("\"com.example.\"", "\"com.example2.\"")
  }

  @Test
  fun `should suggest package for classname attribute`() {
    // given
    myFixture.addFileToProject(
      "com/example/MyClass.java",
      """
      package com.example;
      public class MyClass {}
      """.trimIndent(),
    )

    // when
    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "test_example",
        classname = "<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("\"com.\"")
  }

  @Test
  fun `should not suggest class names outside classname attribute`() {
    // given
    myFixture.addFileToProject(
      "com/example/MyClass.java",
      """
      package com.example;
      public class MyClass {}
      """.trimIndent(),
    )

    // when
    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = com.example.<caret>
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }

    // then
    lookups.shouldBeEmpty()
  }
}
