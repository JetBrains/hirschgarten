package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.target.TestBuildTargetFactory
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtNamedFunction

class SyntheticRunTargetTemplateGeneratorTest : BasePlatformTestCase() {

  fun `test getSyntheticTargetLabel generates valid label`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:lib"))

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
    val label = generator.getSyntheticTargetLabel(target, myFixture.elementAtCaret)

    label.toString() shouldContain ".bazelbsp/synthetic_targets"
    label.toString() shouldContain "synthetic_binary"
  }

  fun `test getSyntheticParams extracts main class name`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:target"))

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
    val params = generator.getSyntheticParams(target, myFixture.elementAtCaret)

    params shouldBe "com.test.TestClass"
  }

  fun `test kotlin getSyntheticParams extracts top level main class name`() {
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("//test:target"))

    myFixture.configureByText(
      "main.kt",
      """
        package com.test

        fun ma<caret>in() {
            println("Hello from main1")
        }
      """.trimIndent()
    )

    val element = (myFixture.elementAtCaret as KtNamedFunction).nameIdentifier!!
    val generator = getDefaultTemplateGenerator(target, KotlinLanguage.INSTANCE)
    val params = generator.getSyntheticParams(target, element)

    params shouldBe "com.test.MainKt"
  }

  fun `test kotlin getSyntheticParams extracts object main class name`() {
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("//test:target"))

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
    val params = generator.getSyntheticParams(target, myFixture.elementAtCaret)

    params shouldBe "com.test.MyObject"
  }

  private fun getDefaultTemplateGenerator(target: RawBuildTarget, language: Language): SyntheticRunTargetTemplateGenerator {
    val generator = SyntheticRunTargetUtils.getTemplateGenerators(target, language)
    generator.size shouldBe 1
    return generator.first()
  }
}
