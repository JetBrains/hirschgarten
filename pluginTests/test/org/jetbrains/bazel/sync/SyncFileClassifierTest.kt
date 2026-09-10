package org.jetbrains.bazel.sync

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

private const val NESTED_REPO = "rules_nested+"
private const val OUTSIDE_REPO = "rules_outside+"
private const val DOWNLOADED_REPO = "rules_downloaded+"

class SyncFileClassifierTest {
  @TempDir
  lateinit var root: Path

  private lateinit var classifier: SyncFileClassifier

  private val workspaceRoot: Path get() = root.resolve("workspace")
  private val outputBase: Path get() = root.resolve("output_base")
  private val nestedRepoRoot: Path get() = workspaceRoot.resolve("third_party/nested")
  private val outsideRepoRoot: Path get() = root.resolve("outside")
  private val downloadedRepoRoot: Path get() = outputBase.resolve("external").resolve(DOWNLOADED_REPO)

  @BeforeEach
  fun setUp() {
    createFile(workspaceRoot.resolve("MODULE.bazel"))
    createFile(workspaceRoot.resolve("WORKSPACE"))
    createFile(workspaceRoot.resolve(".bazelproject"))
    createFile(workspaceRoot.resolve(".bazelrc"))
    createFile(workspaceRoot.resolve("base/BUILD.bazel"))
    createFile(workspaceRoot.resolve("base/src/Base.java"))
    createFile(workspaceRoot.resolve("base/rules.bzl"))
    createFile(workspaceRoot.resolve("docs/notes.md"))
    createFile(workspaceRoot.resolve("docs/orphan_rules.bzl"))
    createFile(nestedRepoRoot.resolve("BUILD.bazel"))
    createFile(nestedRepoRoot.resolve("lib/BUILD"))
    createFile(nestedRepoRoot.resolve("lib/Lib.java"))
    createFile(nestedRepoRoot.resolve("lib/rules.bzl"))
    createFile(outsideRepoRoot.resolve("BUILD.bazel"))
    createFile(outsideRepoRoot.resolve("Outside.java"))
    createFile(downloadedRepoRoot.resolve("BUILD.bazel"))
    createFile(root.resolve("elsewhere/Other.java"))
    classifier = SyncFileClassifier(createRepoMapping(), createBazelInfo())
  }

  @Test
  fun `classifies a BUILD file as every rule target of its package`() {
    val file = classifier.classify(workspaceRoot.resolve("base/BUILD.bazel"))

    file.kind.shouldBeBuild("@//base:all")
    file.isWorkspaceFile shouldBe true
  }

  @Test
  fun `classifies a directory as every rule target beneath it`() {
    val file = classifier.classify(workspaceRoot.resolve("base"))

    file.kind.shouldBeFolder("@//base/...:all")
    file.isWorkspaceFile shouldBe true
  }

  @Test
  fun `classifies the workspace root as every rule target of the workspace`() {
    classifier.classify(workspaceRoot).kind.shouldBeFolder("@//...:all")
  }

  @Test
  fun `classifies a directory of a nested local repository against that repository`() {
    classifier.classify(nestedRepoRoot.resolve("lib")).kind.shouldBeFolder("@@$NESTED_REPO//lib/...:all")
  }

  @Test
  fun `classifies a directory of a downloaded repository as external`() {
    val file = classifier.classify(downloadedRepoRoot)

    file.kind shouldBe SyncFileKind.External
    file.isWorkspaceFile shouldBe false
  }

  @Test
  fun `classifies a source file against the package that holds it`() {
    val file = classifier.classify(workspaceRoot.resolve("base/src/Base.java"))

    file.kind.shouldBeSource("@//base:src/Base.java")
  }

  @Test
  fun `classifies a module file and a workspace file`() {
    classifier.classify(workspaceRoot.resolve("MODULE.bazel")).kind shouldBe SyncFileKind.Module("")
    classifier.classify(workspaceRoot.resolve("WORKSPACE")).kind shouldBe SyncFileKind.Workspace
  }

  @Test
  fun `classifies a project view file and a dot file`() {
    classifier.classify(workspaceRoot.resolve(".bazelproject")).kind shouldBe SyncFileKind.ProjectView
    classifier.classify(workspaceRoot.resolve(".bazelrc")).kind shouldBe SyncFileKind.BazelDotFile
  }

  @Test
  fun `classifies a Starlark file against the package that holds it`() {
    val file = classifier.classify(workspaceRoot.resolve("base/rules.bzl"))

    file.kind.shouldBeStarlark("@//base:rules.bzl")
  }

  @Test
  fun `classifies a Starlark file of a nested local repository against that repository`() {
    val file = classifier.classify(nestedRepoRoot.resolve("lib/rules.bzl"))

    file.kind.shouldBeStarlark("@@$NESTED_REPO//lib:rules.bzl")
  }

  @Test
  fun `classifies a Starlark file that no package holds without a label`() {
    val file = classifier.classify(workspaceRoot.resolve("docs/orphan_rules.bzl"))

    file.kind.shouldBeStarlark(null)
    file.isWorkspaceFile shouldBe true
  }

  @Test
  fun `classifies a file of a nested local repository against that repository`() {
    val file = classifier.classify(nestedRepoRoot.resolve("lib/Lib.java"))

    file.kind.shouldBeSource("@@$NESTED_REPO//lib:Lib.java")
  }

  @Test
  fun `classifies a file of a local repository outside the workspace root as a workspace file`() {
    val file = classifier.classify(outsideRepoRoot.resolve("Outside.java"))

    file.kind.shouldBeSource("@@$OUTSIDE_REPO//:Outside.java")
    file.isWorkspaceFile shouldBe true
  }

  @Test
  fun `classifies a file that no package holds as external`() {
    val file = classifier.classify(workspaceRoot.resolve("docs/notes.md"))

    file.kind shouldBe SyncFileKind.External
    file.isWorkspaceFile shouldBe true
  }

  @Test
  fun `classifies a file of a downloaded repository as external`() {
    val file = classifier.classify(downloadedRepoRoot.resolve("BUILD.bazel"))

    file.kind shouldBe SyncFileKind.External
    file.isWorkspaceFile shouldBe false
  }

  @Test
  fun `classifies a file outside every repository as external`() {
    val file = classifier.classify(root.resolve("elsewhere/Other.java"))

    file.kind shouldBe SyncFileKind.External
    file.isWorkspaceFile shouldBe false
  }

  private fun SyncFileKind.shouldBeBuild(label: String) {
    (this as? SyncFileKind.Build)?.pattern?.toString() shouldBe label
  }

  private fun SyncFileKind.shouldBeFolder(label: String) {
    this.shouldBeTypeOf<SyncFileKind.Folder>().pattern.toString() shouldBe label
  }

  private fun SyncFileKind.shouldBeSource(label: String) {
    (this as? SyncFileKind.Source)?.label?.toString() shouldBe label
  }

  private fun SyncFileKind.shouldBeStarlark(label: String?) {
    this.shouldBeTypeOf<SyncFileKind.Starlark>().label?.toString() shouldBe label
  }

  private fun createFile(path: Path) {
    path.createParentDirectories().writeText("")
  }

  private fun createRepoMapping(): BzlmodRepoMapping =
    BzlmodRepoMapping(
      canonicalRepoNameToLocalPath = mapOf(NESTED_REPO to Path("third_party/nested"), OUTSIDE_REPO to outsideRepoRoot),
      apparentRepoNameToCanonicalName = mapOf("" to "", "nested" to NESTED_REPO, "outside" to OUTSIDE_REPO),
      canonicalRepoNameToPath = mapOf(
        "" to workspaceRoot,
        NESTED_REPO to Path("third_party/nested"),
        OUTSIDE_REPO to outsideRepoRoot,
        DOWNLOADED_REPO to downloadedRepoRoot,
      ),
      nonLocalCanonicalRepoNames = setOf(DOWNLOADED_REPO),
    )

  private fun createBazelInfo(): BazelInfo =
    BazelInfo(
      execRoot = outputBase.resolve("execroot"),
      outputBase = outputBase,
      workspaceRoot = workspaceRoot,
      bazelBin = outputBase.resolve("bazel-bin"),
      release = BazelRelease(9, 0, 0),
      isBzlModEnabled = true,
      isWorkspaceEnabled = false,
      externalAutoloads = emptyList(),
    )
}
