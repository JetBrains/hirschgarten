package org.jetbrains.bazel.run.synthetic

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.java.run.JavaSyntheticRunTargetTemplateGenerator
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.target.TestBuildTargetFactory

class MainClassSyntheticRunTargetTemplateGeneratorTest : BasePlatformTestCase() {

  fun `test getSyntheticTargetLabel generates valid label`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:lib"))
    val generator = JavaSyntheticRunTargetTemplateGenerator()

    myFixture.configureByText(
      "App.java",
      """
      package com.example;

      public class App {
        public static void m<caret>ain(String[] args) {}
      }
      """.trimIndent()
    )

    val label = generator.getSyntheticTargetLabel(target, myFixture.elementAtCaret)

    label.toString() shouldContain ".bazelbsp/synthetic_targets"
    label.toString() shouldContain "synthetic_binary"
  }

  fun `test getSyntheticParams extracts main class name`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:target"))
    val generator = JavaSyntheticRunTargetTemplateGenerator()

    myFixture.configureByText(
      "TestClass.java",
      """
      package com.test;

      public class TestClass {
        public static void m<caret>ain(String[] args) {}
      }
      """.trimIndent()
    )

    val params = generator.getSyntheticParams(target, myFixture.elementAtCaret)

    params shouldBe "com.test.TestClass"
  }

  fun `test createSyntheticTemplate generates template structure`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:target"))
    val generator = JavaSyntheticRunTargetTemplateGenerator()
    val mainClass = "com.example.Main"

    val template = generator.createSyntheticTemplate(target, mainClass)

    template.buildFileContent shouldContain mainClass
    template.buildFileContent shouldContain target.id.toString()
  }

  fun `test getTargetPath generates escaped path`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//my-module:test-target"))
    val generator = JavaSyntheticRunTargetTemplateGenerator()
    val mainClass = "com.example.Main"

    val path = generator.getTargetPath(target, mainClass)

    path.size shouldBe 2
    path[0] shouldContain "_my_module_test_target"
    path[1] shouldContain "com_example_Main"
  }
}
