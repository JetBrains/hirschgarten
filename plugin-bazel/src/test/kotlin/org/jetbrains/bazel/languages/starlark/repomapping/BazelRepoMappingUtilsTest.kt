package org.jetbrains.bazel.languages.starlark.repomapping

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.utils.toVirtualFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path

@RunWith(JUnit4::class)
class BazelRepoMappingUtilsTest : BasePlatformTestCase() {
  override fun setUp() {
    super.setUp()
    project.rootDir = Path.of(getTestDataPath()).toVirtualFile()!! // No idea why this is needed now
    BazelRepoMappingService.getInstance(project).apparentRepoNameToCanonicalName = mapOf("" to "", "repo" to "repo~", "repo2" to "repo2+")
  }

  @Test
  fun `toShortString should not include @ if not needed`() {
    val label = Label.parse("@@//path/to/target")
    label.toShortString(project) shouldBe "//path/to/target"
  }

  @Test
  fun `toShortString should include @ if needed`() {
    val label = Label.parse("@rules_blah//path/to/target:targetName")
    label.toShortString(project) shouldBe "@rules_blah//path/to/target:targetName"
  }

  @Test
  fun `toShortString should not include the target twice`() {
    val label = Label.parse("@//path/to/target:target")
    label.toShortString(project) shouldBe "//path/to/target"
  }

  @Test
  fun `toShortString should use apparent names`() {
    val label = Label.parse("@@repo2+//path/to/target:targetName")
    label.toShortString(project) shouldBe "@repo2//path/to/target:targetName"
  }

  @Test
  fun `toShortString should keep the canonical name if the apparent name can't be resolved`() {
    val label = Label.parse("@@repo_non_existent//path/to/target:targetName")
    label.toShortString(project) shouldBe "@@repo_non_existent//path/to/target:targetName"
  }

  @Test
  fun `toShortString should work for AmbiguousEmptyTarget`() {
    val label = Label.parse("@//path/to/target")
    label.toShortString(project) shouldBe "//path/to/target"
  }

  @Test
  fun `toApparentLabelOrThis should return this on failure`() {
    val label = Label.parse("@@repo_non_existent//path/to/target")
    label.toApparentLabelOrThis(project) shouldBe label
  }

  @Test
  fun `toCanonicalLabel should expand AmbiguousEmptyTarget`() {
    val label = Label.parse("//path/to/target")
    label.toCanonicalLabel(project) shouldBe Label.parse("@@//path/to/target:target")
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
}
