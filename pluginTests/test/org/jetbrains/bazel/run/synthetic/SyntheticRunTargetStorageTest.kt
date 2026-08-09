package org.jetbrains.bazel.run.synthetic

import com.intellij.execution.PsiLocation
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.actionSystem.impl.Utils
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.test.framework.target.TestBuildTargetFactory
import org.jetbrains.bazel.ui.gutters.BazelRunLocation
import org.jetbrains.bazel.ui.gutters.getExecutorActions
import org.jetbrains.kotlin.idea.KotlinLanguage

class SyntheticRunTargetStorageTest : BasePlatformTestCase() {

  override fun setUp() {
    super.setUp()
    Registry.get(BazelFeatureFlags.SYNTHETIC_RUN_ENABLE).setValue(true, testRootDisposable)
  }

  fun `test getSyntheticTargetLabel with single package part`() {
    val label = SyntheticRunTargetUtils.getSyntheticTargetLabel(
      packageParts = arrayOf("simple"),
      targetName = "test_binary",
    )

    label.shouldBeInstanceOf<ResolvedLabel>()
    label.repo.shouldBe(Main)
    label.packagePath.shouldBe(Package(listOf(Constants.DOT_BAZELBSP_DIR_NAME, Constants.SYNTHETIC_TARGETS_DIR_NAME, "simple")))
    label.targetName.shouldBe("test_binary")
  }

  fun `test getSyntheticTargetLabel with multiple package parts`() {

    val label = SyntheticRunTargetUtils.getSyntheticTargetLabel(
      packageParts = arrayOf("part1", "part2", "part3"),
      targetName = "my_target",
    )

    label.shouldBeInstanceOf<ResolvedLabel>()
    label.repo.shouldBe(Main)
    label.packagePath.shouldBe(
      Package(
        listOf(
          Constants.DOT_BAZELBSP_DIR_NAME,
          Constants.SYNTHETIC_TARGETS_DIR_NAME,
          "part1",
          "part2",
          "part3",
        ),
      ),
    )
    label.targetName.shouldBe("my_target")
  }

  fun `test getTemplateGenerators for java target`() {
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = Label.parse("//test:java_lib"))

    val generator = SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, JavaLanguage.INSTANCE)
    generator.shouldNotBeNull()
    generator.isSupported(target) shouldBe true
  }

  fun `test getTemplateGenerators for kotlin target`() {
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("//test:kotlin_lib"))

    val generator = SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, KotlinLanguage.INSTANCE)

    generator.shouldNotBeNull()
    generator.isSupported(target) shouldBe true
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
    val generator = SyntheticRunTargetTemplateGenerator.getTemplateGenerator(javaTarget, KotlinLanguage.INSTANCE)
    generator.shouldBeNull()
  }

  fun `test addSyntheticRunActions creates actions for kotlin library main`() {
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("//test:kotlin_lib"))

    myFixture.configureByText(
      "main.kt",
      """
      package com.test

      fun ma<caret>in() {
        println("Hello")
      }
      """.trimIndent(),
    )
    val actions = getExecutorActions(BazelRunLocation(target, PsiLocation(myFixture.elementAtCaret.firstChild)))
    val group = DefaultActionGroup(actions)
    val factory = PresentationFactory()
    val actionTexts = Utils.expandActionGroup(
      group, factory, SimpleDataContext.getProjectContext(project), ActionPlaces.UNKNOWN, ActionUiKind.NONE,
    ).map { action: AnAction ->
      factory.getPresentation(action).text
    }
    actionTexts shouldContainAll listOf("Run '//test:kotlin_lib (synthetic)'", "Debug '//test:kotlin_lib (synthetic)'")
  }

  fun `test synthetic target label structure`() {
    val label = SyntheticRunTargetUtils.getSyntheticTargetLabel(
      packageParts = arrayOf("escaped_target", "escaped_main"),
      targetName = "synthetic_binary",
    )

    label.shouldBeInstanceOf<ResolvedLabel>()
    label.repo.shouldBe(Main)
    label.packagePath.shouldBe(
      Package(
        listOf(
          Constants.DOT_BAZELBSP_DIR_NAME,
          Constants.SYNTHETIC_TARGETS_DIR_NAME,
          "escaped_target",
          "escaped_main",
        ),
      ),
    )
    label.targetName.shouldBe("synthetic_binary")
  }
}
