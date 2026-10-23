package org.jetbrains.bazel.commons.symlinks

import org.jetbrains.bazel.symlinks.SymlinkTestConditions
import org.jetbrains.bazel.symlinks.createJunction
import org.jetbrains.bazel.symlinks.createSymlinkOrJunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIf
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.name

internal class BazelConvenienceSymlinksScannerTest {
  @TempDir
  lateinit var workspaceRoot: Path

  @BeforeEach
  fun createWorkspaceFile() {
    workspaceRoot.resolve("MODULE.bazel").createFile()
  }

  @Test
  fun `finds a convenience symlink`() {
    val link = createSymlinkOrJunctionInWorkspace("bazel-bin", "execroot/_main/bazel-out/fastbuild/bin")
    assertEquals(setOf(link), scanSymlinks())
  }

  @Test
  fun `finds a symlink with another prefix`() {
    val link = createSymlinkOrJunctionInWorkspace("custom-bin", "execroot/_main/bazel-out/fastbuild/bin")
    assertEquals(setOf(link), scanSymlinks())
  }

  @Test
  fun `finds a symlink one level below the workspace root`() {
    val link = createSymlinkOrJunctionInWorkspace("out/bazel-bin", "execroot/_main/bazel-out/fastbuild/bin")
    assertEquals(setOf(link), scanSymlinks())
  }

  @Test
  fun `finds a symlink that points to the execution root`() {
    val link = createSymlinkOrJunctionInWorkspace("bazel-${workspaceRoot.name}", "execroot/_main")
    assertEquals(setOf(link), scanSymlinks())
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  fun `finds a Windows junction`() {
    val target = workspaceRoot.resolve("execroot/_main/bazel-out")
    val junction = workspaceRoot.resolve("bazel-out").createJunction(target)
    assertEquals(setOf(junction), scanSymlinks())
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  fun `finds a Windows junction whose target is missing`() {
    val target = workspaceRoot.resolve("execroot/_main/bazel-out")
    val junction = workspaceRoot.resolve("bazel-out").createJunction(target)
    target.deleteExisting()
    assertEquals(setOf(junction), scanSymlinks())
  }

  @Test
  @DisabledIf(SymlinkTestConditions.IS_FILE_SYSTEM_CASE_SENSITIVE)
  fun `finds a symlink when the execroot case differs`() {
    val link = createSymlinkOrJunctionInWorkspace("BAZEL-BIN", "EXECROOT/_main/bazel-out/fastbuild/bin")
    assertEquals(setOf(link), scanSymlinks())
  }

  @Test
  @DisabledIf(SymlinkTestConditions.IS_FILE_SYSTEM_CASE_SENSITIVE)
  fun `finds a symlink that points to the execution root when case differs`() {
    val link = createSymlinkOrJunctionInWorkspace("bazel-${workspaceRoot.name.uppercase()}", "execroot/_main")
    assertEquals(setOf(link), scanSymlinks())
  }

  @Test
  @EnabledIf(SymlinkTestConditions.IS_FILE_SYSTEM_CASE_SENSITIVE)
  fun `ignores a symlink when the case differs on a case-sensitive file system`() {
    createSymlinkOrJunctionInWorkspace("bazel-${workspaceRoot.name.uppercase()}", "execroot/_main")
    assertEquals(emptySet<Path>(), scanSymlinks())
  }

  @Test
  fun `ignores a symlink outside the execution root`() {
    workspaceRoot.resolve("bazel-bin").createSymlinkOrJunction(workspaceRoot.root)
    assertEquals(emptySet<Path>(), scanSymlinks())
  }

  @Test
  fun `ignores a symlink with another name`() {
    createSymlinkOrJunctionInWorkspace("generated", "execroot/_main/bazel-out")
    assertEquals(emptySet<Path>(), scanSymlinks())
  }

  @Test
  fun `finds a symlink whose target is missing`() {
    val target = "execroot/_main/bazel-out/fastbuild/bin"
    val link = createSymlinkOrJunctionInWorkspace("bazel-bin", target)
    Files.delete(workspaceRoot.resolve(target))
    assertEquals(setOf(link), scanSymlinks())
  }

  @Test
  fun `ignores a symlink below the maximum depth`() {
    createSymlinkOrJunctionInWorkspace(link = "out/nested/bazel-bin", target = "execroot/_main/bazel-out/fastbuild/bin")
    assertEquals(emptySet<Path>(), scanSymlinks())
  }

  @Test
  fun `finds a path under a symlink as deep as the scan`(@TempDir otherDir: Path) {
    createSymlinkOrJunctionInWorkspace("bazel-bin", "execroot/_main/bazel-out/fastbuild/bin")
    createSymlinkOrJunctionInWorkspace("out/bazel-out", "execroot/_main/bazel-out")
    createSymlinkOrJunctionInWorkspace("out/nested/bazel-testlogs", "execroot/_main/bazel-out/fastbuild/testlogs")
    workspaceRoot.resolve("src").createDirectories()
    val scanner = BazelConvenienceSymlinksScanner(workspaceRoot, MAX_DEPTH)

    assertEquals(
      listOf(true, true, false, false, false, false),
      listOf(
        scanner.isUnderBazelSymlink(workspaceRoot.resolve("bazel-bin/pkg")),
        scanner.isUnderBazelSymlink(workspaceRoot.resolve("out/bazel-out/pkg")),
        // deeper than the maximum depth
        scanner.isUnderBazelSymlink(workspaceRoot.resolve("out/nested/bazel-testlogs/pkg")),
        scanner.isUnderBazelSymlink(workspaceRoot.resolve("src/pkg")),
        scanner.isUnderBazelSymlink(workspaceRoot),
        scanner.isUnderBazelSymlink(otherDir.resolve("bazel-bin/pkg")),
      ),
    )
  }

  private fun scanSymlinks(): Set<Path> = BazelConvenienceSymlinksScanner(workspaceRoot, MAX_DEPTH).scan()

  private fun createSymlinkOrJunctionInWorkspace(link: String, target: String): Path {
    val targetPath = workspaceRoot.resolve(target).createDirectories()
    val linkPath = workspaceRoot.resolve(link)
    linkPath.parent.createDirectories()
    return linkPath.createSymlinkOrJunction(targetPath)
  }

  private companion object {
    // The default value of the bazel.symlink.scan.max.depth registry key
    const val MAX_DEPTH = 2
  }
}
