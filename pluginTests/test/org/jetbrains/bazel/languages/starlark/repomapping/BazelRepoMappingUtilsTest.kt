package org.jetbrains.bazel.languages.starlark.repomapping

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.languages.starlark.references.findReferredPackage
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.createDirectory
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelRepoMappingUtilsTest {
  private val testBazel by testBazelProjectFixture()

  private val project get() = testBazel.project

  @BeforeEach
  fun setUp() {
    testBazel.createDirectory("my/repo/package")
    testBazel.createDirectory("repo2")
    testBazel.sync {
      repositories {
        local("repo~", "my/repo", apparentName = "repo")
        local("repo2+", "repo2", apparentName = "repo2")
      }
    }
  }

  @Test
  fun `toShortString should not include @ if not needed`() {
    val label = Label.parse("@@//path/to/target")
    label.assumeResolved().toShortString(project) shouldBe "//path/to/target"
  }

  @Test
  fun `toShortString should include @ if needed`() {
    val label = Label.parse("@rules_blah//path/to/target:targetName")
    label.assumeResolved().toShortString(project) shouldBe "@rules_blah//path/to/target:targetName"
  }

  @Test
  fun `toShortString should not include the target twice`() {
    val label = Label.parse("@//path/to/target:target")
    label.assumeResolved().toShortString(project) shouldBe "//path/to/target"
  }

  @Test
  fun `toShortString should use apparent names`() {
    val label = Label.parse("@@repo2+//path/to/target:targetName")
    label.assumeResolved().toShortString(project) shouldBe "@repo2//path/to/target:targetName"
  }

  @Test
  fun `toShortString should keep the canonical name if the apparent name can't be resolved`() {
    val label = Label.parse("@@repo_non_existent//path/to/target:targetName")
    label.assumeResolved().toShortString(project) shouldBe "@@repo_non_existent//path/to/target:targetName"
  }

  @Test
  fun `toShortString should work for AmbiguousEmptyTarget`() {
    val label = Label.parse("@//path/to/target")
    label.assumeResolved().toShortString(project) shouldBe "//path/to/target"
  }

  @Test
  fun `toApparentLabel should return null on failure`() {
    val label = Label.parse("@@repo_non_existent//path/to/target")
    label.toApparentLabel(project) shouldBe null
  }

  @Test
  fun `toCanonicalLabel should expand AmbiguousEmptyTarget`() {
    val label = Label.parse("//path/to/target")
    label.toCanonicalLabel(project) shouldBe Label.parse("//path/to/target:target")
  }

  @Test
  fun `toCanonicalLabel should canonicalize the repo`() {
    val label = Label.parse("@repo//path/to/target:targetName")
    label.toCanonicalLabel(project) shouldBe Label.parse("@@repo~//path/to/target:targetName")
  }

  @Test
  fun `toCanonicalLabel should return null on failure`() {
    val label = Label.parse("@repo_non_existent//path/to/target:targetName")
    label.toCanonicalLabel(project) shouldBe null
  }

  @Test
  fun `findReferredPackage should work with wildcard labels`() {
    val label = Label.parse("@repo//package/...")
    findReferredPackage(project, label.assumeResolved()) shouldBe project.rootDir.findFileByRelativePath("my/repo/package")!!
  }
}
