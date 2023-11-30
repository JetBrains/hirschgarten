package org.jetbrains.bazel.extension

import io.kotest.matchers.shouldBe
import org.junit.Test

class BazelBuildTargetClassifierTest {
  private val classifier = BazelBuildTargetClassifier()

  @Test
  fun mainRepoTest() {
    val targetId = "@//a/b/c:label"
    classifier.calculateBuildTargetName(targetId) shouldBe "label"
    classifier.calculateBuildTargetPath(targetId) shouldBe listOf("a", "b", "c")
  }

  @Test
  fun mainRepoTestOldSyntax() { // pre-bazel 6 syntax
    val targetId = "//a/b/c:label"
    classifier.calculateBuildTargetName(targetId) shouldBe "label"
    classifier.calculateBuildTargetPath(targetId) shouldBe listOf("a", "b", "c")
  }

  @Test
  fun emptyPath() { // pre-bazel 6 syntax
    val targetId = "//:label"
    classifier.calculateBuildTargetName(targetId) shouldBe "label"
    classifier.calculateBuildTargetPath(targetId) shouldBe listOf()
  }

  @Test
  fun labelNotMatchingBazelPattern() {
    val targetId = "foo"
    classifier.calculateBuildTargetName(targetId) shouldBe "foo"
    classifier.calculateBuildTargetPath(targetId) shouldBe listOf()
  }

  @Test
  fun labelNotMatchingBazelPatternWithSlash() {
    // this test documents behavior, it is unclear what should be the result for this kind of label
    val targetId = "foo/bar"
    classifier.calculateBuildTargetName(targetId) shouldBe "foo/bar"
    classifier.calculateBuildTargetPath(targetId) shouldBe listOf()
  }
}
