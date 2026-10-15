package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.lookup.LookupElement
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.setTargets
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelTargetCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project

  private fun setTargets(targets: List<String>) {
    project.setTargets(
      targets.map {
        TestBuildTarget(
          key = WorkspaceTargetKey(label = Label.parse(it)),
          dependencies = emptyList(),
          kind =
            TargetKind(
              kind = "java_library",
              ruleType = RuleType.LIBRARY,
              languageClasses = setOf(JavaLanguageClass.JAVA),
            ),
          sources = OutputLocationCollection.EMPTY,
          resources = OutputLocationCollection.EMPTY,
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
    val lookups = completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"//target1\"", "\"//target2\"")
  }

  @Test
  fun `should not complete outside`() {
    setTargets(listOf("//target1", "//target2"))

    myFixture.configureByText("BUILD", "")
    myFixture.type("\"")
    val lookups = completeBasic().flatMap { it.allLookupStrings }

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
    val lookups = completeBasic().flatMap { it.allLookupStrings }

    myFixture.type("3")
    val lookups2 = completeBasic().flatMap { it.allLookupStrings }

    myFixture.type("4")
    val lookups3 = completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"", "\"//abcd1239\"", "\"//abcd1299\"")
    lookups2 shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"", "\"//abcd1239\"")
    lookups3 shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"")
  }

  private fun completeBasic(): Array<LookupElement> = myFixture.completeBasic()
}
