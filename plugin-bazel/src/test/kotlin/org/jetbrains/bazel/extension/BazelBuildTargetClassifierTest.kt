package org.jetbrains.bazel.extension

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.extensionPoints.BazelBuildTargetClassifier
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.junit.jupiter.api.Test

class BazelBuildTargetClassifierTest {
  private val classifier = BazelBuildTargetClassifier

  @Test
  fun mainRepoTest() {
    val targetInfo = "@//a/b/c:label".toBuildTargetInfo()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "label"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf("a", "b", "c")
  }

  @Test
  fun mainRepoTestOldSyntax() { // pre-bazel 6 syntax
    val targetInfo = "//a/b/c:label".toBuildTargetInfo()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "label"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf("a", "b", "c")
  }

  @Test
  fun emptyPath() { // pre-bazel 6 syntax
    val targetInfo = "//:label".toBuildTargetInfo()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "label"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf()
  }

  @Test
  fun labelNotMatchingBazelPattern() {
    val targetInfo = "foo".toBuildTargetInfo()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "foo"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf("foo")
  }

  @Test
  fun labelNotMatchingBazelPatternWithSlash() {
    // this test documents behavior, it is unclear what should be the result for this kind of label
    val targetInfo = "foo/bar".toBuildTargetInfo()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "bar"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf("foo", "bar")
  }
}

private fun String.toBuildTargetInfo() = BuildTargetInfo(id = Label.parse(this))
