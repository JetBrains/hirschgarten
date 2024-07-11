package org.jetbrains.bazel.extension

import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.junit.jupiter.api.Test

class BazelBuildTargetClassifierTest {
  private val classifier = BazelBuildTargetClassifier()

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
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf()
  }

  @Test
  fun labelNotMatchingBazelPatternWithSlash() {
    // this test documents behavior, it is unclear what should be the result for this kind of label
    val targetInfo = "foo/bar".toBuildTargetInfo()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "foo/bar"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf()
  }
}

private fun String.toBuildTargetInfo() = BuildTargetInfo(id = this)
