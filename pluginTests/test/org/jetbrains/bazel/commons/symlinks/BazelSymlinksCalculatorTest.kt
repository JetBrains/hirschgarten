package org.jetbrains.bazel.commons.symlinks

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.IoTestUtil
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

internal class BazelSymlinksCalculatorTest {
  @TempDir
  lateinit var workspaceRoot: Path

  @BeforeEach
  fun createWorkspaceFile() {
    workspaceRoot.resolve("MODULE.bazel").createFile()
  }

  @Test
  fun `finds a convenience symlink`() {
    val link = createSymlinkOrJunctionInWorkspace("bazel-bin", "execroot/_main/bazel-out/fastbuild/bin")
    assertEquals(setOf(link), calculateSymlinksToExclude())
  }

  @Test
  fun `finds a symlink with another prefix`() {
    val link = createSymlinkOrJunctionInWorkspace("custom-bin", "execroot/_main/bazel-out/fastbuild/bin")
    assertEquals(setOf(link), calculateSymlinksToExclude())
  }

  @Test
  fun `finds a symlink one level below the workspace root`() {
    val link = createSymlinkOrJunctionInWorkspace("out/bazel-bin", "execroot/_main/bazel-out/fastbuild/bin")
    assertEquals(setOf(link), calculateSymlinksToExclude())
  }

  @Test
  fun `finds a symlink that points to the execution root`() {
    val link = createSymlinkOrJunctionInWorkspace("bazel-${workspaceRoot.name}", "execroot/_main")
    assertEquals(setOf(link), calculateSymlinksToExclude())
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  fun `finds a Windows junction`() {
    val target = workspaceRoot.resolve("execroot/_main/bazel-out")
    val junction = workspaceRoot.resolve("bazel-out").createJunction(target)
    assertEquals(setOf(junction), calculateSymlinksToExclude())
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  fun `finds a Windows junction whose target is missing`() {
    val target = workspaceRoot.resolve("execroot/_main/bazel-out")
    val junction = workspaceRoot.resolve("bazel-out").createJunction(target)
    target.deleteExisting()
    assertEquals(setOf(junction), calculateSymlinksToExclude())
  }

  @Test
  @DisabledIf("isFsCaseSensitive")
  fun `finds a symlink when the execroot case differs`() {
    val link = createSymlinkOrJunctionInWorkspace("BAZEL-BIN", "EXECROOT/_main/bazel-out/fastbuild/bin")
    assertEquals(setOf(link), calculateSymlinksToExclude())
  }

  @Test
  @DisabledIf("isFsCaseSensitive")
  fun `finds a symlink that points to the execution root when case differs`() {
    IoTestUtil.assumeCaseInsensitiveFS()
    val link = createSymlinkOrJunctionInWorkspace("bazel-${workspaceRoot.name.uppercase()}", "execroot/_main")
    assertEquals(setOf(link), calculateSymlinksToExclude())
  }

  @Test
  @EnabledIf("isFsCaseSensitive")
  fun `ignores a symlink when the case differs on a case-sensitive file system`() {
    createSymlinkOrJunctionInWorkspace("bazel-${workspaceRoot.name.uppercase()}", "execroot/_main")
    assertEquals(emptySet<Path>(), calculateSymlinksToExclude())
  }

  @Test
  fun `ignores a symlink outside the execution root`() {
    workspaceRoot.resolve("bazel-bin").createSymlinkOrJunction(workspaceRoot.root)
    assertEquals(emptySet<Path>(), calculateSymlinksToExclude())
  }

  @Test
  fun `ignores a symlink with another name`() {
    createSymlinkOrJunctionInWorkspace("generated", "execroot/_main/bazel-out")
    assertEquals(emptySet<Path>(), calculateSymlinksToExclude())
  }

  @Test
  fun `finds a symlink whose target is missing`() {
    val target = "execroot/_main/bazel-out/fastbuild/bin"
    val link = createSymlinkOrJunctionInWorkspace("bazel-bin", target)
    Files.delete(workspaceRoot.resolve(target))
    assertEquals(setOf(link), calculateSymlinksToExclude())
  }

  @Test
  fun `ignores a symlink below the maximum depth`() {
    createSymlinkOrJunctionInWorkspace(link = "out/nested/bazel-bin", target = "execroot/_main/bazel-out/fastbuild/bin")
    assertEquals(emptySet<Path>(), calculateSymlinksToExclude())
  }

  private fun calculateSymlinksToExclude(): Set<Path> =
    BazelSymlinksCalculator.calculateBazelSymlinksToExclude(workspaceRoot, MAX_DEPTH)

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

  private fun isFsCaseSensitive(): Boolean = SystemInfoRt.isFileSystemCaseSensitive
}
