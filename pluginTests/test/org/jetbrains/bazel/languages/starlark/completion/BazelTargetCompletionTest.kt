package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.Path

@RunWith(JUnit4::class)
class BazelTargetCompletionTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    project.isBazelProject = true
  }

  private fun setTargets(targets: List<String>) {
    project.targetUtils.setTargets(
      targets.map {
        RawBuildTarget(
          id = Label.parse(it),
          tags = emptyList(),
          dependencies = emptyList(),
          kind =
            TargetKind(
              kindString = "java_library",
              ruleType = RuleType.LIBRARY,
              languageClasses = setOf(LanguageClass.JAVA),
            ),
          sources = emptyList(),
          resources = emptyList(),
          baseDirectory = Path("/"),
        )
      },
    )
  }

  @Test
  fun `should complete in deps`() {
    setTargets(listOf("//target1", "//target2"))

    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
        name = "test",
        srcs = glob(["*.kt",]),
        visibility = [],
        deps = [
          "//another_test_target",
          <caret>
        ],
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"//target1\"", "\"//target2\"")
  }

  @Test
  fun `should not complete outside`() {
    setTargets(listOf("//target1", "//target2"))

    myFixture.configureByText("BUILD", "")
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups.shouldBeEmpty()
  }

  @Test
  fun `should search for targets`() {
    setTargets(listOf("//abcd1299", "//abcd1239", "//abcd1234"))

    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(deps = [<caret>],)
      """.trimMargin(),
    )
    myFixture.type("\"12")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    myFixture.type("3")
    val lookups2 = myFixture.completeBasic().flatMap { it.allLookupStrings }

    myFixture.type("4")
    val lookups3 = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"", "\"//abcd1239\"", "\"//abcd1299\"")
    lookups2 shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"", "\"//abcd1239\"")
    lookups3 shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"")
  }
}
