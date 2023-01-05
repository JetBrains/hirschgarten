package org.jetbrains.plugins.bsp.extensions

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.extension.points.TemporaryTestTargetClassifier
import org.junit.jupiter.api.Test

class TemporaryTestTargetClassifierTest {
  private val classifier = TemporaryTestTargetClassifier()
  @Test
  fun mainRepoTest() {
    val target = makeTarget("@//a/b/c:label")
    classifier.getBuildTargetName(target) shouldBe "label"
    classifier.getBuildTargetPath(target) shouldBe listOf("a", "b", "c")
  }
  @Test
  fun mainRepoTestOldSyntax() { // pre-bazel 6 syntax
    val target = makeTarget("//a/b/c:label")
    classifier.getBuildTargetName(target) shouldBe "label"
    classifier.getBuildTargetPath(target) shouldBe listOf("a", "b", "c")
  }

  @Test
  fun emptyPath() { // pre-bazel 6 syntax
    val target = makeTarget("//:label")
    classifier.getBuildTargetName(target) shouldBe "label"
    classifier.getBuildTargetPath(target) shouldBe listOf()
  }
  @Test
  fun labelNotMatchingBazelPattern() {
    val target = makeTarget("foo")
    classifier.getBuildTargetName(target) shouldBe "foo"
    classifier.getBuildTargetPath(target) shouldBe listOf()
  }

  @Test
  fun labelNotMatchingBazelPatternWithSlash() {
    // this test documents behavior, it is unclear what should be the result for this kind of label
    val target = makeTarget("foo/bar")
    classifier.getBuildTargetName(target) shouldBe "foo/bar"
    classifier.getBuildTargetPath(target) shouldBe listOf()
  }

  private fun makeTarget(label: String) =
    BuildTarget(BuildTargetIdentifier(label), emptyList(), emptyList(), emptyList(), BuildTargetCapabilities())
}