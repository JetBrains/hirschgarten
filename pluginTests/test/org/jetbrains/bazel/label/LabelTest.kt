package org.jetbrains.bazel.label

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
    label.packagePath.toString() shouldBe "path/to/target"
    label.assumeResolved().repo shouldBe Main
    label.isMainWorkspace shouldBe true
  }

  @Test
  fun `should return target path for label with bazel 6 target`() {
    val label = Label.parse("@//path/to/target:targetName")
    val targetPath = label.packagePath.toString()
    targetPath shouldBe "path/to/target"
  }

  @Test
  fun `should return repo name`() {
    val label = Label.parse("@rules_blah//path/to/target:targetName")
    val repoName = label.assumeResolved().repoName
    repoName shouldBe "rules_blah"
    label.isApparent shouldBe true
  }

  @Test
  fun `should parse repo name in canonical form`() {
    val label = Label.parse("@@rules_blah~~something~//path/to/target:targetName")
    val repoName = label.assumeResolved().repoName
    repoName shouldBe "rules_blah~~something~"
    label.isApparent shouldBe false
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
  fun `should correctly parse at-less targets`() {
    val label = Label.parse("//path/to/target:targetName")
    val normalized = label.toString()
    normalized shouldBe "@//path/to/target:targetName"
    label.targetName shouldBe "targetName"
    label.packagePath.toString() shouldBe "path/to/target"
    label.assumeResolved().repo shouldBe Main
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
  fun `main workspace targets should be equal, no matter how they are created`() {
    val label1 = Label.parse("@//path/to/target:target")
    val label2 = Label.parse("//path/to/target:target")
    val label3 = Label.parse("@@//path/to/target:target")

    val label4 = Label.parse("@@//path/to/target")
    val label5 = Label.parse("//path/to/target")
    label1 shouldBe label2
    label1 shouldBe label3

    label4 shouldBe label5
  }

  @Test
  fun `synthetic labels should be considered main workspace`() {
    val label = Label.synthetic("scala-compiler-2.12.14.jar")
    label.isMainWorkspace shouldBe true
  }

  @Test
  fun `relative labels are resolved on a best-effort basis`() {
    val label = Label.parse(":target").assumeResolved()
    label.toString() shouldBe "@//:target"
    label.targetName shouldBe "target"
    label.packagePath.toString() shouldBe ""
    label.repo shouldBe Main
    label.isMainWorkspace shouldBe true

    val label2 = Label.parse("target").assumeResolved()
    label2.toString() shouldBe "@//target"
    label2.targetName shouldBe "target"
    label2.target shouldBe AmbiguousEmptyTarget
    label2.packagePath.toString() shouldBe "target"
    label.repo shouldBe Main
    label2.isMainWorkspace shouldBe true
  }

  @Test
  fun `all targets and wildcard are the same`() {
    val label1 = Label.parse(":all-targets")
    val label2 = Label.parse(":*")
    label1 shouldBe label2
  }

  @Test
  fun `all targets is normalized to wildcard`() {
    val label = Label.parse("//:all-targets")
    label.toString() shouldBe "@//:*"
  }

  @Test
  fun `package wildcards are normalized`() {
    val label = Label.parse("//...:all")
    label.toString() shouldBe "@//...:all"

    val label2 = Label.parse("//...")
    label2.toString() shouldBe "@//...:all"

    label shouldBe label2
  }

  @Test
  fun `package parent function works`() {
    val label = Label.parse("@//path/to/target:target")
    val parent = (label.packagePath as Package).parent()
    parent.toString() shouldBe "path/to"
  }

  @Test
  fun `all rules targets are parsed correctly`() {
    val label = Label.parse("@//path/to/target:all")
    label.target shouldBe AllRuleTargets
  }

  @Test
  fun `all rule targets and files are parsed correctly`() {
    val label = Label.parse("@//path/to/target:*")
    label.target shouldBe AllRuleTargetsAndFiles
  }

  @Test
  fun `package wildcards are parsed correctly`() {
    val label = Label.parse("@//path/to/...")
    label.packagePath shouldBe AllPackagesBeneath(listOf("path", "to"))
    label.target shouldBe AllRuleTargets
    label.toString() shouldBe "@//path/to/...:all"
  }

  @Test
  fun `package wildcard without target is the same as all rule targets`() {
    val label1 = Label.parse("@//path/to/...")
    val label2 = Label.parse("@//path/to/...:all")
    label1 shouldBe label2
  }

  @Test
  fun `package wildcard with all-targets is the same as asterisk`() {
    val label1 = Label.parse("@//path/to/...:*")
    val label2 = Label.parse("path/to/...:all-targets").assumeResolved()
    label1 shouldBe label2
  }

  @Test
  fun `synthetic label should be parsed correctly`() {
    val label = Label.parse("scala-compiler-2.12.14.jar[synthetic]")
    label.isSynthetic shouldBe true
  }

  @Test
  fun `parsing relative labels works`() {
    val label = Label.parse(":target")
    label.toString() shouldBe ":target"
    label.targetName shouldBe "target"
    label.packagePath.toString() shouldBe ""
    label.isRelative shouldBe true
  }

  @Test
  fun `resolving relative labels works`() {
    val base = Label.parse("@//path/to").assumeResolved()
    val relative = Label.parse(":target").asRelative()
    val resolved = relative?.resolve(base)
    resolved.toString() shouldBe "@//path/to:target"
  }

  @Test
  fun `resolving works in the more complex case`() {
    val base = Label.parse("@blah//path/to").assumeResolved()
    val relative = Label.parse("path/segment:oh/god").asRelative()
    val resolved = relative?.resolve(base)
    resolved.toString() shouldBe "@blah//path/to/path/segment:oh/god"
    resolved?.target shouldBe SingleTarget("oh/god")
  }

  @Test
  fun `resolving a relative label with respect to a wildcard package is impossible`() {
    val base = Label.parse("@blah//path/to/...").assumeResolved()
    val relative = Label.parse("path/segment").asRelative()
    shouldThrow<IllegalStateException> {
      relative?.resolve(base)
    }
  }

  @Test
  fun `resolving a relative label preserves empty ambigous target`() {
    val base = Label.parse("@blah//path/to").assumeResolved()
    val relative = Label.parse("another/path").asRelative()!!
    val resolved = relative.resolve(base)
    resolved.toString() shouldBe "@blah//path/to/another/path"
    resolved.target shouldBe AmbiguousEmptyTarget
    resolved.packagePath shouldBe Package(listOf("path", "to", "another", "path"))
  }

  @Test
  fun `parse relative label with ambiguous package`() {
    val label1 = Label.parse("src/Hello.java")
    label1.toString() shouldBe "src/Hello.java"
    label1.target shouldBe AmbiguousEmptyTarget
    label1.packagePath shouldBe Package(listOf("src", "Hello.java"))
    label1.isRelative shouldBe true

    val label2 = Label.parse("//src/Hello.java")
    label2.toString() shouldBe "@//src/Hello.java"
    label2.target shouldBe AmbiguousEmptyTarget
    label2.packagePath shouldBe Package(listOf("src", "Hello.java"))
    label2.isRelative shouldBe false
  }

  @Test
  fun `targetName returns package name for ambiguous empty target`() {
    val label = Label.parse("//src/Hello.java")
    label.targetName shouldBe "Hello.java"
  }

  @ParameterizedTest
  @ValueSource(
    strings = ["@//path/to/target", "@//path:all", "relative:target", "relative:all", "wildcard/...", "@@canonical~//path/to/target"],
  )
  fun `toString preserves semantics`() {
    val label = Label.parse("@//path/to/target")
    val string = label.toString()
    val parsed = Label.parse(string)
    parsed shouldBe label
  }

  @Test
  fun `three dots can only be used with wildcard targets`() {
    shouldThrow<IllegalArgumentException> {
      Label.parse("@//path/to/...:target")
    }
  }

  @Test
  fun `parseOrNull returns null on invalid labels`() {
    Label.parseOrNull("AUTH: successfully authenticated").shouldBeNull()
    Label.parseOrNull("Bazel exited with code 123").shouldBeNull()
  }

  @Test
  fun `should parse label with leading whitespaces`() {
    Label.parse("   @//path/to/target    ") shouldBe Label.parse("@//path/to/target")
  }
}
