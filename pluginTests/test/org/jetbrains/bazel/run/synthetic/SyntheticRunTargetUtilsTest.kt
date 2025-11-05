package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.java.JavaLanguage
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.test.framework.target.TestBuildTargetFactory
import org.jetbrains.kotlin.idea.KotlinLanguage

class SyntheticRunTargetUtilsTest : BasePlatformTestCase() {

  fun `test getSyntheticTargetLabel with single package part`() {
    val label = SyntheticRunTargetUtils.getSyntheticTargetLabel(
      packageParts = arrayOf("simple"),
      targetName = "test_binary"
    )

    label.shouldBeInstanceOf<ResolvedLabel>()
    label.repo.shouldBe(Main)
    label.packagePath.shouldBe(Package(listOf(".bazelbsp", "synthetic_targets", "simple")))
    label.targetName.shouldBe("test_binary")
  }

  fun `test getSyntheticTargetLabel with multiple package parts`() {

    val label = SyntheticRunTargetUtils.getSyntheticTargetLabel(
      packageParts = arrayOf("part1", "part2", "part3"),
      targetName = "my_target"
    )

    label.shouldBeInstanceOf<ResolvedLabel>()
    label.repo.shouldBe(Main)
    label.packagePath.shouldBe(Package(listOf(".bazelbsp", "synthetic_targets", "part1", "part2", "part3")))
    label.targetName.shouldBe("my_target")
  }

  fun `test getTemplateGenerators for java target`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:java_lib"))

    val generators = SyntheticRunTargetUtils.getTemplateGenerators(target, JavaLanguage.INSTANCE)

    generators.shouldNotBeEmpty()
    generators.all { it.isSupported(target) }.shouldBe(true)
  }

  fun `test getTemplateGenerators for kotlin target`() {
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("//test:kotlin_lib"))

    val generators = SyntheticRunTargetUtils.getTemplateGenerators(target, KotlinLanguage.INSTANCE)

    generators.shouldNotBeEmpty()
    generators.all { it.isSupported(target) }.shouldBe(true)
  }

  fun `test escapeTargetLabel with special characters`() {
    SyntheticRunTargetUtils.escapeTargetLabel("//foo:bar").shouldBe("__foo_bar")
    SyntheticRunTargetUtils.escapeTargetLabel("@repo//pkg:target").shouldBe("_repo__pkg_target")
    SyntheticRunTargetUtils.escapeTargetLabel("my-target-123").shouldBe("my_target_123")
    SyntheticRunTargetUtils.escapeTargetLabel("Target.With.Dots").shouldBe("Target_With_Dots")
    SyntheticRunTargetUtils.escapeTargetLabel("normal_name_123").shouldBe("normal_name_123")
  }

  fun `test getTemplateGenerators filters unsupported generators`() {
    val javaTarget = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:java"))
    val generators = SyntheticRunTargetUtils.getTemplateGenerators(javaTarget, KotlinLanguage.INSTANCE)

    generators.shouldBeEmpty()
  }

  fun `test synthetic target label structure`() {
    val label = SyntheticRunTargetUtils.getSyntheticTargetLabel(
      packageParts = arrayOf("escaped_target", "escaped_main"),
      targetName = "synthetic_binary"
    )

    label.shouldBeInstanceOf<ResolvedLabel>()
    label.repo.shouldBe(Main)
    label.packagePath.shouldBe(Package(listOf(".bazelbsp", "synthetic_targets", "escaped_target", "escaped_main")))
    label.targetName.shouldBe("synthetic_binary")
  }
}
