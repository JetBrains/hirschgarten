package org.jetbrains.bazel.extension

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.TreeTargetClassifier
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TreeTargetClassifierTest : WorkspaceModelBaseTest() {
  lateinit var classifier: TreeTargetClassifier

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    classifier = TreeTargetClassifier(project)
  }

  @Test
  fun mainRepoTest() {
    val targetInfo = "@//a/b/c:label".toLabel()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "label"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf("a", "b", "c")
  }

  @Test
  fun mainRepoTestOldSyntax() { // pre-bazel 6 syntax
    val targetInfo = "//a/b/c:label".toLabel()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "label"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf("a", "b", "c")
  }

  @Test
  fun emptyPath() { // pre-bazel 6 syntax
    val targetInfo = "//:label".toLabel()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "label"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf()
  }

  @Test
  fun labelNotMatchingBazelPattern() {
    val targetInfo = "foo".toLabel()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "foo"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf("foo")
  }

  @Test
  fun labelNotMatchingBazelPatternWithSlash() {
    // this test documents behavior, it is unclear what should be the result for this kind of label
    val targetInfo = "foo/bar".toLabel()
    classifier.calculateBuildTargetName(targetInfo) shouldBe "bar"
    classifier.calculateBuildTargetPath(targetInfo) shouldBe listOf("foo", "bar")
  }
}

private fun String.toLabel() = Label.parse(this)
