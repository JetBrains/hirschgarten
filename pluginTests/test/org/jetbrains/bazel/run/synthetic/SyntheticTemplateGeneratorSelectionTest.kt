package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.java.JavaLanguage
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.java.run.JavaSyntheticRunTargetTemplateGenerator
import org.jetbrains.bazel.kotlin.run.KotlinSyntheticRunTargetTemplateGenerator
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.kotlinLibrary
import org.jetbrains.bsp.protocol.id
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.jupiter.api.Test

@BazelTestApplication
class SyntheticTemplateGeneratorSelectionTest {
  @Test
  fun `test java template generator selection`() {
    val target = javaLibrary("target")
    val generator = SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, JavaLanguage.INSTANCE)

    generator.shouldNotBeNull()
    generator.shouldBeInstanceOf<JavaSyntheticRunTargetTemplateGenerator>()
  }

  @Test
  fun `test kotlin template generator selection`() {
    val target = kotlinLibrary("target")
    val generator = SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, KotlinLanguage.INSTANCE)

    generator.shouldNotBeNull()
    generator.shouldBeInstanceOf<KotlinSyntheticRunTargetTemplateGenerator>()
  }

  @Test
  fun `test java main in kotlin target template generator selection`() {
    val target = kotlinLibrary("target")
    val generator = SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, JavaLanguage.INSTANCE)

    generator.shouldNotBeNull()
    generator.shouldBeInstanceOf<JavaSyntheticRunTargetTemplateGenerator>()
  }

  @Test
  fun `test java generator supports jvm targets`() {
    val target = javaLibrary("//test:target")
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(JavaLanguage.INSTANCE)

    generator.isSupported(target).shouldBe(true)
  }

  @Test
  fun `test kotlin generator supports kotlin targets`() {
    val target = kotlinLibrary("//test:target")
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(KotlinLanguage.INSTANCE)

    generator.isSupported(target).shouldBe(true)
  }

  @Test
  fun `test java generator creates valid build content`() {
    val target = javaLibrary("//test:target")
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(JavaLanguage.INSTANCE)
    val mainClass = "com.example.Main"

    val template = generator.createSyntheticTemplate(target, SyntheticRunTargetTemplateGenerator.Params(mainClass))

    template.buildFileContent.shouldContain("java_binary")
    template.buildFileContent.shouldContain("main_class = \"$mainClass\"")
    template.buildFileContent.shouldContain("runtime_deps = [\"${target.id}\"]")
    template.buildFileContent.shouldContain("@rules_java//java:defs.bzl")
  }

  @Test
  fun `test kotlin generator creates valid build content`() {
    val target = kotlinLibrary("//test:target")
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(KotlinLanguage.INSTANCE)
    val mainClass = "com.example.MainKt"

    val template = generator.createSyntheticTemplate(target, SyntheticRunTargetTemplateGenerator.Params(mainClass))

    template.buildFileContent.shouldContain("kt_jvm_binary")
    template.buildFileContent.shouldContain("main_class = \"$mainClass\"")
    template.buildFileContent.shouldContain("runtime_deps = [\"${target.id}\"]")
    template.buildFileContent.shouldContain("@rules_kotlin//kotlin:jvm.bzl")
  }

  @Test
  fun `test target label escaping`() {
    val input = "//my-module:test@target"
    val escaped = SyntheticRunTargetUtils.escapeTargetLabel(input)

    escaped.shouldBe("__my_module_test_target")
    escaped.shouldNotBe(input)
  }

  @Test
  fun `test target label escaping preserves alphanumeric and underscore`() {
    val input = "valid_Target123"
    val escaped = SyntheticRunTargetUtils.escapeTargetLabel(input)

    escaped.shouldBe(input)
  }

  @Test
  fun `test synthetic template has correct build file path`() {
    val target = javaLibrary("//test:lib")
    val generator = SyntheticRunTargetTemplateGenerator.ep.forLanguage(JavaLanguage.INSTANCE)
    val mainClass = "com.example.App"

    val template = generator.createSyntheticTemplate(target, SyntheticRunTargetTemplateGenerator.Params(mainClass))

    template.buildFilePath.shouldNotBe("")
    template.buildFilePath.shouldContain("test_lib")
    template.buildFilePath.shouldContain("com_example_App")
  }

}
