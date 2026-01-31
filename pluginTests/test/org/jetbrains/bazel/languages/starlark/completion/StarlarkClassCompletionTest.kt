package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
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

    lookups.shouldContain("\"com.\"")
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

    lookups.shouldContain("\"com.example.MyClass.NestedClass\"")
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
    lookups.shouldContain("\"com.example.MyClass\"")
  }

  @Test
  fun `should not suggest abstract class and interface`() {
    myFixture.addFileToProject(
      "com/example/AbstractBase.java",
      """
      package com.example;
      public abstract class AbstractBase {}
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/MyInterface.java",
      """
      package com.example;
      public interface MyInterface {}
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/Valid.java",
      """
      package com.example;
      public class Valid extends AbstractBase implements MyInterface {}
      """.trimIndent(),
    )

    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "t",
        classname = "com.example.<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }
    lookups.shouldContainOnly("\"com.example.Valid\"")
  }

  @Test
  fun `should not suggest class marked synthetic by Kotlin Metadata`() {
    myFixture.addFileToProject(
      "com/example/SyntheticByMetadata.java",
      """
      package com.example;
      @kotlin.Metadata(k = 3, mv = {1, 8, 0})
      public class SyntheticByMetadata {}
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/Valid.kt",
      """
      package com.example
      class Valid
      """.trimIndent(),
    )

    myFixture.configureByText(
      "BUILD",
      """
      kt_jvm_binary(
        name = "t",
        main_class = "com.example.<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }
    lookups.shouldContainOnly("\"com.example.Valid\"")
  }

  @Test
  fun `should not suggest DefaultImpls class`() {
    myFixture.addFileToProject(
      "com/example/WithDefaults${'$'}DefaultImpls.java",
      """
      package com.example;
      public final class WithDefaults${'$'}DefaultImpls {}
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/Valid.java",
      """
      package com.example;
      public class Valid {}
      """.trimIndent(),
    )

    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "t",
        classname = "com.example.<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }
    lookups.shouldContainOnly("\"com.example.Valid\"")
  }

  @Test
  fun `should not suggest multi-file part-like class`() {
    myFixture.addFileToProject(
      "com/example/mf/FooKt__A.java",
      """
      package com.example.mf;
      public class FooKt__A {}
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/mf/Valid.java",
      """
      package com.example.mf;
      public class Valid {}
      """.trimIndent(),
    )

    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "t",
        classname = "com.example.mf.<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }
    lookups.shouldContainOnly("\"com.example.mf.Valid\"")
  }

  @Test
  fun `should not suggest classes with double-dollar in name`() {
    myFixture.addFileToProject(
      "com/example/Some${"$$"}Lambda$1.java",
      """
      package com.example;
      public class Some${"$$"}Lambda$1 {}
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "com/example/Valid.java",
      """
      package com.example;
      public class Valid {}
      """.trimIndent(),
    )

    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "t",
        classname = "com.example.<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }
    lookups.shouldContainOnly("\"com.example.Valid\"")
  }
}
