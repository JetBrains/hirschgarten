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

    lookups.shouldContainExactlyInAnyOrder(listOf("\"com.example.MyClass\"", "\"com.example.MyOtherClass\""))
  }

  @Test
  fun `should suggest kt classes for classname attribute`() {
    myFixture.addFileToProject(
      "com/example/MyClass.kt",
      """
      package com.example
      class MyClass
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/MyOtherClass.kt",
      """
      package com.example
      class MyOtherClass
      """.trimIndent(),
    )

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

    lookups.shouldContainExactlyInAnyOrder(listOf("\"com.example.MyClass\"", "\"com.example.MyOtherClass\""))
  }

  @Test
  fun `should suggest subpackage for classname attribute`() {
    myFixture.addFileToProject(
      "com/example/MyClass.java",
      """
      package com.example;
      public class MyClass {}
      """.trimIndent(),
    )

    myFixture.addFileToProject(
      "com/exampleKt/MyKtClass.kt",
      """
      package com.exampleKt
      class MyKtClass
      """.trimIndent(),
    )

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

    lookups.shouldContainExactlyInAnyOrder(listOf("\"com.example.\"", "\"com.exampleKt.\""))
  }

  @Test
  fun `should suggest package for classname attribute`() {
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
        classname = "<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }

    lookups.shouldContainExactlyInAnyOrder(listOf("\"com.\""))
  }

  @Test
  fun `should suggest two versions of completing the class with a nested class`() {
    myFixture.addFileToProject(
      "com/example/MyClass.kt",
      """
      package com.example
      class MyClass  {
        class NestedClass 
      }
      """.trimIndent(),
    )

    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "test_example",
        classname = "com.example.My<caret>",
      )
      """.trimMargin(),
    )
    val lookups = myFixture.completeBasic().map { it.lookupString }

    lookups.shouldContainExactlyInAnyOrder(listOf("\"com.example.MyClass\"", "\"com.example.MyClass.\""))
  }

  @Test
  fun `should suggest nested class`() {
    myFixture.addFileToProject(
      "com/example/MyClass.kt",
      """
      package com.example
      class MyClass  {
        class NestedClass 
      }
      """.trimIndent(),
    )

    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "test_example",
        classname = "com.example.MyClass.<caret>",
      )
      """.trimMargin(),
    )
    val lookups = myFixture.completeBasic().map { it.lookupString }

    lookups.shouldContainExactlyInAnyOrder(listOf("\"com.example.MyClass.NestedClass\""))
  }

  @Test
  fun `should not suggest class names outside classname attribute`() {
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
        name = com.example.<caret>
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }

    lookups.shouldBeEmpty()
  }

  @Test
  fun `should insert selected class and finish classname completion`() {
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
        classname = "com.example.My<caret>",
      )
      """.trimMargin(),
    )
    myFixture.completeBasic()
    myFixture.type('\n')

    myFixture.checkResult(
      """
      java_binary(
        name = "test_example",
        classname = "com.example.MyClass"<caret>,
      )
      """.trimMargin(),
    )
  }

  @Test
  fun `should insert selected subpackage and continue completion`() {
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
        classname = "com.<caret>",
      )
      """.trimMargin(),
    )

    myFixture.completeBasic()
    myFixture.type('\n')
    val lookups = myFixture.completeBasic().map { it.lookupString }

    myFixture.checkResult(
      """
      java_binary(
        name = "test_example",
        classname = "com.example.<caret>",
      )
      """.trimMargin(),
    )
    lookups.shouldContainExactlyInAnyOrder(listOf("\"com.example.MyClass\""))
  }
}
