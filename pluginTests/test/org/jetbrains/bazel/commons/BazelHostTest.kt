package org.jetbrains.bazel.commons

import com.intellij.platform.eel.EelOsFamily
import com.intellij.platform.eel.EelPathBoundDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths

class BazelHostTest {
  @TempDir
  lateinit var tempDir: Path

  /**
   * A Posix environment whose root the IDE sees as [tempDir],
   * like the IDE sees the root of a WSL distribution as `\\wsl.localhost\Ubuntu`.
   */
  private val posixHost: BazelHost by lazy { BazelHost(PosixTestDescriptor(tempDir)) }

  @Test
  fun `local host keeps paths as they are`() {
    val path = tempDir.resolve("dir").resolve("file.txt")

    assertTrue(BazelHost.LOCAL.isLocal)
    assertEquals(path, BazelHost.LOCAL.toIdePath(path.toString()))
    assertEquals(path.toString(), BazelHost.LOCAL.toHostPath(path))
    assertEquals(path, BazelHost.LOCAL.uriToIdePath(path.toUri().toString()))
    assertEquals(Paths.get("relative", "file.txt"), BazelHost.LOCAL.toIdePathAllowingRelative("relative/file.txt"))
  }

  @Test
  fun `host of a local path is the local host`() {
    assertSame(BazelHost.LOCAL, BazelHost.of(tempDir))
  }

  @Test
  fun `absolute host path resolves under the root of the host`() {
    assertFalse(posixHost.isLocal)
    assertEquals(tempDir.resolve("home").resolve("user").resolve("project"), posixHost.toIdePath("/home/user/project"))
  }

  @Test
  fun `host path that is not absolute has no IDE path`() {
    assertNull(posixHost.toIdePathOrNull("relative/file.txt"))
    assertNull(posixHost.toIdePathOrNull(""))
  }

  @Test
  fun `file URI of the host resolves under the root of the host`() {
    assertEquals(
      tempDir.resolve("home").resolve("user").resolve("test.xml"),
      posixHost.uriToIdePath("file:///home/user/test.xml"),
    )
  }

  @Test
  fun `relative host path stays relative`() {
    assertEquals(Paths.get("bazel-out", "bin", "lib.jar"), posixHost.toIdePathAllowingRelative("bazel-out/bin/lib.jar"))
  }

  @Test
  fun `absolute host path converts when a relative path is allowed`() {
    assertEquals(tempDir.resolve("usr").resolve("lib").resolve("jvm"), posixHost.toIdePathAllowingRelative("/usr/lib/jvm"))
  }

  @Test
  fun `relative path goes to a Posix host with forward slashes`() {
    assertEquals("bazel-out/bin/lib.jar", posixHost.toHostPath(Paths.get("bazel-out", "bin", "lib.jar")))
  }

  @Test
  fun `absolute path form follows the system of the host`() {
    assertTrue(posixHost.isAbsoluteHostPath("/home/user"))
    assertFalse(posixHost.isAbsoluteHostPath("home/user"))
    assertFalse(posixHost.isAbsoluteHostPath("C:\\Users"))

    val windowsHost = BazelHost(WindowsTestDescriptor(tempDir))
    assertTrue(windowsHost.isAbsoluteHostPath("C:\\Users"))
    assertTrue(windowsHost.isAbsoluteHostPath("C:/Users"))
    assertFalse(windowsHost.isAbsoluteHostPath("/home/user"))
  }

  @Test
  fun `executable name and separators follow the system of the host`() {
    assertEquals("java", posixHost.executableName("java"))
    assertEquals(":", posixHost.pathSeparator)
    assertEquals("/", posixHost.fileSeparator)

    val windowsHost = BazelHost(WindowsTestDescriptor(tempDir))
    assertEquals("java.exe", windowsHost.executableName("java"))
    assertEquals(";", windowsHost.pathSeparator)
    assertEquals("\\", windowsHost.fileSeparator)
  }

  private class PosixTestDescriptor(override val rootPath: Path) : EelPathBoundDescriptor {
    override val name: String = "Posix test environment"
    override val osFamily: EelOsFamily = EelOsFamily.Posix
  }

  private class WindowsTestDescriptor(override val rootPath: Path) : EelPathBoundDescriptor {
    override val name: String = "Windows test environment"
    override val osFamily: EelOsFamily = EelOsFamily.Windows
  }
}
