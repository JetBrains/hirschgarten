package org.jetbrains.bsp.bazel.server.sync.model

import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.bazel.server.model.Label
import org.junit.jupiter.api.Test

class LabelTest {
  @Test
  fun `should normalize @@ targets`() {
    // given
    val label = Label.parse("@@//path/to/target:targetName")

    // when
    val normalized = label.toString()

    // then
    normalized shouldBe "@//path/to/target:targetName"
  }

  @Test
  fun `should correctly parse @-less targets`() {
    // given
    val label = Label.parse("//path/to/target:targetName")

    // when
    val normalized = label.toString()

    // then
    normalized shouldBe "@//path/to/target:targetName"
    label.targetName shouldBe "targetName"
    label.targetPath shouldBe "path/to/target"
    label.repoName shouldBe ""
    label.isMainWorkspace shouldBe true
  }

  @Test
  fun `should return target path for label with bazel 6 target`() {
    // given
    val label = Label.parse("@//path/to/target:targetName")

    // when
    val targetPath = label.targetPath

    // then
    targetPath shouldBe "path/to/target"
  }

  @Test
  fun `should return repo name`() {
    // given
    val label = Label.parse("@rules_blah//path/to/target:targetName")

    // when
    val repoName = label.repoName

    // then
    repoName shouldBe "rules_blah"
  }

  @Test
  fun `should correctly identify main workspace`() {
    // given
    val label = Label.parse("@//path/to/target:targetName")

    // when
    val isMainWorkspace = label.isMainWorkspace

    // then
    isMainWorkspace shouldBe true
  }

  @Test
  fun `should correctly identify non-main workspace`() {
    // given
    val label = Label.parse("@rules_blah//path/to/target:targetName")

    // when
    val isMainWorkspace = label.isMainWorkspace

    // then
    isMainWorkspace shouldBe false
  }

  @Test
  fun `should return target name for label with bazel 6 target`() {
    // given
    val label = Label.parse("@//path/to/target:targetName")

    // when
    val targetName = label.targetName

    // then
    targetName shouldBe "targetName"
  }

  @Test
  fun `should leave the special cased scala compiler label alone`() {
    // given
    val label = Label.parse("scala-compiler-2.12.14.jar")

    // when
    val normalized = label.toString()

    // then
    normalized shouldBe "scala-compiler-2.12.14.jar"
  }

  @Test
  fun `should return target name for label with bazel 5 target`() {
    // given
    val label = Label.parse("//path/to/target:targetName")

    // when
    val targetName = label.targetName

    // then
    targetName shouldBe "targetName"
  }

  @Test
  fun `should return empty string for label with target without target name`() {
    // given
    val label = Label.parse("//path/to/target")

    // when
    val targetName = label.targetName

    // then
    targetName shouldBe ""
  }
}
