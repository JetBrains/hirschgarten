package org.jetbrains.bazel.languages.starlark.repomapping

import com.intellij.openapi.application.runWriteAction
import com.intellij.testFramework.replaceService
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.languages.starlark.references.findReferredPackage
import org.jetbrains.bazel.workspace.BazelRepoMappingService
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

class BazelRepoMappingUtilsTest : WorkspaceModelBaseTest() {
  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    val rootDir = project.rootDir.toNioPath()
    runWriteAction {
      project.rootDir
        .createChildDirectory(this, "my")
        .createChildDirectory(this, "repo")
        .createChildDirectory(this, "package")
      project.rootDir.createChildDirectory(this, "repo2")
    }
    val repoMappingService = object : BazelRepoMappingService {
      override val apparentRepoNameToCanonicalName: Map<String, String>
        get() = mapOf("" to "", "repo" to "repo~", "repo2" to "repo2+")
      override val canonicalRepoNameToApparentName: Map<String, String>
        get() = mapOf("" to "", "repo~" to "repo", "repo2+" to "repo2")
      override val canonicalRepoNameToPath: Map<String, Path>
        get() = mapOf("" to rootDir, "repo~" to rootDir.resolve("my", "repo"), "repo2+" to rootDir.resolve("repo2"))
    }
    project.replaceService(BazelRepoMappingService::class.java, repoMappingService, disposable)
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
