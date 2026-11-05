package org.jetbrains.bazel.sync

import com.intellij.openapi.util.io.NioFiles
import com.intellij.util.system.OS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class FindExecutableInPathTest {
  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `skips a directory with the name of the executable`() {
    val first = Files.createDirectories(tempDir.resolve("first"))
    Files.createDirectories(first.resolve(executableName("bazel")))
    val bazel = executable(tempDir.resolve("second"), "bazel")

    assertEquals(bazel, findExecutableInPath(pathOf(first, bazel.parent), "bazel", "bazelisk"))
  }

  @Test
  fun `the first directory with any of the names wins`() {
    val bazelisk = executable(tempDir.resolve("first"), "bazelisk")
    val bazel = executable(tempDir.resolve("second"), "bazel")

    assertEquals(bazelisk, findExecutableInPath(pathOf(bazelisk.parent, bazel.parent), "bazel", "bazelisk"))
  }

  @Test
  fun `in one directory the order of the names decides`() {
    val dir = tempDir.resolve("dir")
    executable(dir, "bazelisk")
    val bazel = executable(dir, "bazel")

    assertEquals(bazel, findExecutableInPath(pathOf(dir), "bazel", "bazelisk"))
  }

  @Test
  fun `skips empty and relative entries`() {
    val bazel = executable(tempDir.resolve("dir"), "bazel")
    val pathValue = listOf("", "relative", bazel.parent.toString()).joinToString(File.pathSeparator)

    assertEquals(bazel, findExecutableInPath(pathValue, "bazel"))
  }

  @Test
  fun `returns null when no directory has an executable`() {
    val dir = Files.createDirectories(tempDir.resolve("dir"))
    Files.createDirectories(dir.resolve(executableName("bazel")))

    assertNull(findExecutableInPath(pathOf(dir), "bazel", "bazelisk"))
  }

  @Test
  @EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
  fun `on Windows skips a file without the exe extension`() {
    val first = Files.createDirectories(tempDir.resolve("first"))
    Files.createFile(first.resolve("bazel"))
    Files.createFile(first.resolve("bazel.bat"))
    val bazel = executable(tempDir.resolve("second"), "bazel")

    assertEquals(bazel, findExecutableInPath(pathOf(first, bazel.parent), "bazel"))
  }

  private fun pathOf(vararg dirs: Path): String = dirs.joinToString(File.pathSeparator)

  private fun executableName(name: String): String = if (OS.CURRENT == OS.Windows) "$name.exe" else name

  private fun executable(dir: Path, name: String): Path {
    val file = Files.createDirectories(dir).resolve(executableName(name))
    Files.createFile(file)
    NioFiles.setExecutable(file)
    return file
  }
}
