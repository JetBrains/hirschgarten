package org.jetbrains.bazel.commons

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.ignore.BazelIgnoreService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.annotation.BazelTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createSymbolicLinkPointingTo

/**
 * [dropExcludedPatternsWithoutPackages] must keep an excluded pattern when Bazel can find a package beneath it.
 * See BAZEL-3586.
 */
@BazelTest
class DropExcludedPatternsWithoutPackagesTest {
  @TempDir
  lateinit var workspaceRoot: Path

  @TempDir
  lateinit var outsideRoot: Path

  @Test
  fun `drops the pattern of a directory without a BUILD file`() {
    createFile("docs/notes.md")
    isKept("//docs/...") shouldBe false
  }

  @Test
  fun `drops the pattern of a missing directory`() {
    isKept("//missing/...") shouldBe false
  }

  @Test
  fun `drops the pattern when only a directory has a BUILD file name`() {
    workspaceRoot.resolve("docs/BUILD").createDirectories()
    isKept("//docs/...") shouldBe false
  }

  @Test
  fun `keeps the pattern of a directory with a BUILD file`() {
    createFile("docs/BUILD")
    isKept("//docs/...") shouldBe true
  }

  @Test
  fun `keeps the pattern of a directory with a nested BUILD file`() {
    createFile("docs/a/b/BUILD.bazel")
    isKept("//docs/...") shouldBe true
  }

  @Test
  fun `keeps the pattern of a nested module`() {
    createFile("docs/inner/MODULE.bazel")
    createFile("docs/inner/BUILD.bazel")
    isKept("//docs/...") shouldBe true
  }

  @Test
  fun `keeps the pattern of the main workspace`() {
    createFile("MODULE.bazel")
    createFile("lib/BUILD.bazel")
    isKept("//...") shouldBe true
  }

  @Test
  fun `drops the pattern when only an ignored subdirectory has a BUILD file`() {
    createFile("docs/ignored/BUILD.bazel")
    isKept("//docs/...", ignoredDirectories = setOf("docs/ignored")) shouldBe false
  }

  @Test
  fun `drops the pattern of an ignored directory`() {
    createFile("docs/BUILD.bazel")
    isKept("//docs/...", ignoredDirectories = setOf("docs")) shouldBe false
  }

  @Test
  @DisabledOnOs(OS.WINDOWS) // symlinks need extra privileges
  fun `keeps the pattern when a child symlink points to a package`() {
    outsideRoot.resolve("pkg").createDirectories().resolve("BUILD.bazel").createFile()
    workspaceRoot.resolve("docs").createDirectories().resolve("link").createSymbolicLinkPointingTo(outsideRoot.resolve("pkg"))
    isKept("//docs/...") shouldBe true
  }

  @Test
  @DisabledOnOs(OS.WINDOWS) // symlinks need extra privileges
  fun `keeps the pattern when the excluded directory is a symlink`() {
    outsideRoot.resolve("pkg").createDirectories().resolve("BUILD.bazel").createFile()
    workspaceRoot.resolve("docs").createSymbolicLinkPointingTo(outsideRoot.resolve("pkg"))
    isKept("//docs/...") shouldBe true
  }

  @Test
  @DisabledOnOs(OS.WINDOWS) // symlinks need extra privileges
  fun `keeps the pattern when a symlink has a BUILD file name`() {
    val target = outsideRoot.resolve("rules.bzl").createFile()
    workspaceRoot.resolve("docs").createDirectories().resolve("BUILD.bazel").createSymbolicLinkPointingTo(target)
    isKept("//docs/...") shouldBe true
  }

  @Test
  @DisabledOnOs(OS.WINDOWS) // symlinks need extra privileges
  fun `drops the pattern when a symlink points to a file without a BUILD file name`() {
    val target = outsideRoot.resolve("notes.md").createFile()
    workspaceRoot.resolve("docs").createDirectories().resolve("notes.md").createSymbolicLinkPointingTo(target)
    isKept("//docs/...") shouldBe false
  }

  @Test
  fun `keeps the other excluded patterns`() {
    val excluded = listOf(Label.parse("//docs:target"), Label.parse("@other//docs/..."), Label.parse("//missing/..."))
    val result = runBlocking {
      TargetCollection(listOf(Label.parse("//...")), excluded).dropExcludedPatternsWithoutPackages(workspaceRoot, ignoring(emptySet()))
    }
    result shouldBe TargetCollection(listOf(Label.parse("//...")), excluded.take(2))
  }

  private fun createFile(relativePath: String) {
    val file = workspaceRoot.resolve(relativePath)
    file.parent.createDirectories()
    file.createFile()
  }

  private fun isKept(pattern: String, ignoredDirectories: Set<String> = emptySet()): Boolean {
    val label = Label.parse(pattern)
    val targets = TargetCollection(listOf(Label.parse("//...")), listOf(label))
    val result = runBlocking { targets.dropExcludedPatternsWithoutPackages(workspaceRoot, ignoring(ignoredDirectories)) }
    return label in result.excludedValues
  }

  private fun ignoring(ignoredDirectories: Set<String>): BazelIgnoreService =
    object : BazelIgnoreService {
      override fun isIgnored(path: Path): Boolean = ignoredDirectories.any { path == workspaceRoot.resolve(it) }
    }
}
