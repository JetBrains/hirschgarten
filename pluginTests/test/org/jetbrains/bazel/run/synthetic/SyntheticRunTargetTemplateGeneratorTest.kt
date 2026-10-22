package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runReadActionBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.kotlinLibrary
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.jupiter.api.Test

@BazelTestApplication
class SyntheticRunTargetTemplateGeneratorTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `test getSyntheticTargetLabel generates valid label`() {
    val target = javaLibrary("//test:lib")

    myFixture.configureByText(
      "App.java",
      """
      package com.example;

      public class App {
        public static void m<caret>ain(String[] args) {}
      }
      """.trimIndent()
    )

    val generator = getDefaultTemplateGenerator(target, JavaLanguage.INSTANCE)
    val label = runReadActionBlocking { generator.getSyntheticTargetLabel(target, myFixture.elementAtCaret) }

    label.toString().shouldContain("${Constants.DOT_BAZELBSP_DIR_NAME}/${Constants.SYNTHETIC_TARGETS_DIR_NAME}")
    label.toString().shouldContain("synthetic_binary")
  }

  @Test
  fun `test getSyntheticParams extracts main class name`() {
    val target = javaLibrary("//test:target")

    myFixture.configureByText(
      "TestClass.java",
      """
      package com.test;

      public class TestClass {
        public static void m<caret>ain(String[] args) {}
      }
      """.trimIndent()
    )

    val generator = getDefaultTemplateGenerator(target, JavaLanguage.INSTANCE)
    val params = runReadActionBlocking { generator.getSyntheticParams(target, myFixture.elementAtCaret) }

    params.data.shouldBe("com.test.TestClass")
  }

  @Test
  fun `test kotlin getSyntheticParams extracts top level main class name`() {
    val target = kotlinLibrary("//test:target")

    myFixture.configureByText(
      "main.kt",
      """
        package com.test

        fun ma<caret>in() {
            println("Hello from main1")
        }
      """.trimIndent()
    )

    val generator = getDefaultTemplateGenerator(target, KotlinLanguage.INSTANCE)
    val params = runReadActionBlocking {
      val element = (myFixture.elementAtCaret as KtNamedFunction).nameIdentifier!!
      generator.getSyntheticParams(target, element)
    }

    params.data.shouldBe("com.test.MainKt")
  }

  @Test
  fun `test kotlin getSyntheticParams extracts object main class name`() {
    val target = kotlinLibrary("//test:target")

    myFixture.configureByText(
      "MyObject.kt",
      """
      package com.test;
      
      object MyObject {
        @JvmStatic
        fun m<caret>ain(args: Array<String>) {
        }
      }
      """.trimIndent()
    )

    val generator = getDefaultTemplateGenerator(target, KotlinLanguage.INSTANCE)
    val params = runReadActionBlocking { generator.getSyntheticParams(target, myFixture.elementAtCaret) }

    params.data.shouldBe("com.test.MyObject")
  }

  private fun getDefaultTemplateGenerator(target: BuildTarget, language: Language): SyntheticRunTargetTemplateGenerator {
    val generator = SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, language)
    generator.shouldNotBeNull()
    return generator
  }
}
