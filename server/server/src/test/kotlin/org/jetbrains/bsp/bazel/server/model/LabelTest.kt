package org.jetbrains.bsp.bazel.server.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LabelTest {

  @Test
  fun `should normalize @@ main repo targets`() {
    val label = Label.parse("@@//path/to/target:targetName")
    val normalized = label.toString()
    normalized shouldBe "@//path/to/target:targetName"
  }

  @Test
  fun `should normalize @@ main repo targets without target name`() {
    val label = Label.parse("@@//path/to/target")
    val normalized = label.toString()
    normalized shouldBe "@//path/to/target"
  }

  @Test
  fun `should normalize canonical repo targets`() {
    val label = Label.parse("@@rules_blah~//path/to/target:targetName")
    val normalized = label.toString()
    normalized shouldBe "@@rules_blah~//path/to/target:targetName"
    }

  @Test
  fun `should correctly parse @-less targets`() {
    val label = Label.parse("//path/to/target:targetName")
    val normalized = label.toString()
    normalized shouldBe "@//path/to/target:targetName"
    label.targetName shouldBe "targetName"
    label.targetPath shouldBe "path/to/target"
    label.repoName shouldBe ""
    label.isMainWorkspace shouldBe true
  }

  @Test
  fun `should return target path for label with bazel 6 target`() {
    val label = Label.parse("@//path/to/target:targetName")
    val targetPath = label.targetPath
    targetPath shouldBe "path/to/target"
  }

  @Test
  fun `should return repo name`() {
    val label = Label.parse("@rules_blah//path/to/target:targetName")
    val repoName = label.repoName
    repoName shouldBe "rules_blah"
  }

  @Test
  fun `should parse repo name in canonical form`() {
    val label = Label.parse("@@rules_blah~~something~//path/to/target:targetName")
    val repoName = label.repoName
    repoName shouldBe "rules_blah~~something~"
  }

  @Test
  fun `should correctly identify main workspace`() {
    val label = Label.parse("@//path/to/target:targetName")
    val isMainWorkspace = label.isMainWorkspace
    isMainWorkspace shouldBe true
  }

  @Test
  fun `should correctly identify non-main workspace`() {
    val label = Label.parse("@rules_blah//path/to/target:targetName")
    val isMainWorkspace = label.isMainWorkspace
    isMainWorkspace shouldBe false
  }

  @Test
  fun `should return target name for label with bazel 6 target`() {
    val label = Label.parse("@//path/to/target:targetName")
    val targetName = label.targetName
    targetName shouldBe "targetName"
  }

  @Test
  fun `should return target name for label with bazel 5 target`() {
    val label = Label.parse("//path/to/target:targetName")
    val targetName = label.targetName
    targetName shouldBe "targetName"
  }

  @Test
  fun `should return empty string for label with target without target name`() {
    val label = Label.parse("//path/to/target")
    val targetName = label.targetName
    targetName shouldBe "target"
  }

  @Test
  fun `should correctly parse at-less targets`() {
    val label = Label.parse("//path/to/target:targetName")
    val normalized = label.toString()
    normalized shouldBe "@//path/to/target:targetName"
    label.targetName shouldBe "targetName"
    label.targetPath shouldBe "path/to/target"
    label.repoName shouldBe ""
    label.isMainWorkspace shouldBe true
  }

  @Test
  fun `should normalize synthetic targets`() {
    val label = Label.synthetic("scala-compiler-2.12.14.jar")
    val normalized = label.toString()
    normalized shouldBe "scala-compiler-2.12.14.jar[synthetic]"
  }

  @Test
  fun `should not add the synthetic tag again`() {
    val label = Label.synthetic("scala-compiler-2.12.14.jar[synthetic]")
    val normalized = label.toString()
    normalized shouldBe "scala-compiler-2.12.14.jar[synthetic]"
  }

  @Test
  fun `should return package name as target for label without explicit target name`() {
    val label = Label.parse("//path/to/target")
    val targetName = label.targetName
    targetName shouldBe "target"
  }

  @Test
  fun `main workspace targets should be equal, no matter how they are created`() {
    val label1 = Label.parse("@//path/to/target:target")
    val label2 = Label.parse("//path/to/target:target")
    val label3 = Label.parse("@@//path/to/target:target")
    val label4 = Label.parse("@@//path/to/target")
    val label5 = Label.parse("//path/to/target")
    label1 shouldBe label2
    label1 shouldBe label3
    label1 shouldBe label4
    label1 shouldBe label5
  }

  @Test
  fun `synthetic labels should be considered main workspace`() {
    val label = Label.synthetic("scala-compiler-2.12.14.jar")
    label.isMainWorkspace shouldBe true
  }

  @Test
  fun `relative labels are parsed on a best-effort basis`() {
    val label = Label.parse(":target")
    label.toString() shouldBe "@//:target"
    label.targetName shouldBe "target"
    label.targetPath shouldBe ""
    label.repoName shouldBe ""
    label.isMainWorkspace shouldBe true

    val label2 = Label.parse("target")
    label2.toString() shouldBe "@//target"
    label2.targetName shouldBe "target"
    label2.targetPath shouldBe "target"
    label2.repoName shouldBe ""
    label2.isMainWorkspace shouldBe true
  }
}
