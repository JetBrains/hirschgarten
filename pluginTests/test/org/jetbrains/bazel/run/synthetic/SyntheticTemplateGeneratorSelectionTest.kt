package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.java.JavaLanguage
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.java.run.JavaSyntheticRunTargetTemplateGenerator
import org.jetbrains.bazel.kotlin.run.KotlinSyntheticRunTargetTemplateGenerator
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.target.TestBuildTargetFactory
import org.jetbrains.kotlin.idea.KotlinLanguage

class SyntheticTemplateGeneratorSelectionTest : BasePlatformTestCase() {

  fun `test java template generator selection`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("target"))
    val generators = SyntheticRunTargetUtils.getTemplateGenerators(target, JavaLanguage.INSTANCE)

    generators.count().shouldBe(1)
    generators.first().shouldBeInstanceOf<JavaSyntheticRunTargetTemplateGenerator>()
  }

  fun `test kotlin template generator selection`() {
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("target"))
    val generators = SyntheticRunTargetUtils.getTemplateGenerators(target, KotlinLanguage.INSTANCE)

    generators.count().shouldBe(1)
    generators.first().shouldBeInstanceOf<KotlinSyntheticRunTargetTemplateGenerator>()
  }

  fun `test java main in kotlin target template generator selection`() {
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("target"))
    val generators = SyntheticRunTargetUtils.getTemplateGenerators(target, JavaLanguage.INSTANCE)

    generators.count().shouldBe(1)
    generators.first().shouldBeInstanceOf<JavaSyntheticRunTargetTemplateGenerator>()
  }

  fun `test java generator supports jvm targets`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:target"))
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(JavaLanguage.INSTANCE)

    generator.isSupported(target).shouldBe(true)
  }

  fun `test kotlin generator supports kotlin targets`() {
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("//test:target"))
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(KotlinLanguage.INSTANCE)

    generator.isSupported(target).shouldBe(true)
  }

  fun `test java generator creates valid build content`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:target"))
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(JavaLanguage.INSTANCE)
    val mainClass = "com.example.Main"

    val template = generator.createSyntheticTemplate(target, SyntheticRunTargetParams.ofString(mainClass))

    template.buildFileContent.shouldContain("java_binary")
    template.buildFileContent.shouldContain("main_class = \"$mainClass\"")
    template.buildFileContent.shouldContain("runtime_deps = [\"${target.id}\"]")
    template.buildFileContent.shouldContain("@rules_java//java:defs.bzl")
  }

  fun `test kotlin generator creates valid build content`() {
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("//test:target"))
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(KotlinLanguage.INSTANCE)
    val mainClass = "com.example.MainKt"

    val template = generator.createSyntheticTemplate(target, SyntheticRunTargetParams.ofString(mainClass))

    template.buildFileContent.shouldContain("kt_jvm_binary")
    template.buildFileContent.shouldContain("main_class = \"$mainClass\"")
    template.buildFileContent.shouldContain("runtime_deps = [\"${target.id}\"]")
    template.buildFileContent.shouldContain("@rules_kotlin//kotlin:jvm.bzl")
  }

  fun `test target label escaping`() {
    val input = "//my-module:test@target"
    val escaped = SyntheticRunTargetUtils.escapeTargetLabel(input)

    escaped.shouldBe("__my_module_test_target")
    escaped.shouldNotBe(input)
  }

  fun `test target label escaping preserves alphanumeric and underscore`() {
    val input = "valid_Target123"
    val escaped = SyntheticRunTargetUtils.escapeTargetLabel(input)

    escaped.shouldBe(input)
  }

  fun `test synthetic template has correct build file path`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:lib"))
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(JavaLanguage.INSTANCE)
    val mainClass = "com.example.App"

    val template = generator.createSyntheticTemplate(target, SyntheticRunTargetParams.ofString(mainClass))

    template.buildFilePath.shouldNotBe("")
    template.buildFilePath.shouldContain("test_lib")
    template.buildFilePath.shouldContain("com_example_App")
  }

}
