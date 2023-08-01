package org.jetbrains.plugins.bsp.extensions

import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.extension.points.TemporaryTestTargetClassifier
import org.junit.jupiter.api.Test

class TemporaryTestTargetClassifierTest {
  private val classifier = TemporaryTestTargetClassifier()

  @Test
  fun mainRepoTest() {
    val targetId = "@//a/b/c:label"
    classifier.getBuildTargetName(targetId) shouldBe "label"
    classifier.getBuildTargetPath(targetId) shouldBe listOf("a", "b", "c")
  }

  @Test
  fun mainRepoTestOldSyntax() { // pre-bazel 6 syntax
    val targetId = "//a/b/c:label"
    classifier.getBuildTargetName(targetId) shouldBe "label"
    classifier.getBuildTargetPath(targetId) shouldBe listOf("a", "b", "c")
  }

  @Test
  fun emptyPath() { // pre-bazel 6 syntax
    val targetId = "//:label"
    classifier.getBuildTargetName(targetId) shouldBe "label"
    classifier.getBuildTargetPath(targetId) shouldBe listOf()
  }

  @Test
  fun labelNotMatchingBazelPattern() {
    val targetId = "foo"
    classifier.getBuildTargetName(targetId) shouldBe "foo"
    classifier.getBuildTargetPath(targetId) shouldBe listOf()
  }

  @Test
  fun labelNotMatchingBazelPatternWithSlash() {
    // this test documents behavior, it is unclear what should be the result for this kind of label
    val targetId = "foo/bar"
    classifier.getBuildTargetName(targetId) shouldBe "foo/bar"
    classifier.getBuildTargetPath(targetId) shouldBe listOf()
  }
}
