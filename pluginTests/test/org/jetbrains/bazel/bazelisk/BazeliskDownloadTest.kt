package org.jetbrains.bazel.bazelisk

import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.jetbrains.bazel.sync.environment.BazeliskDownload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BazeliskDownloadTest {
  @Test
  fun `GitHub names the release asset of the platform`() {
    val version = BazeliskDownload.GitHub.VERSION
    val releaseUrl = "https://github.com/bazelbuild/bazelisk/releases/download/v$version"

    assertEquals("$releaseUrl/bazelisk-linux-amd64", BazeliskDownload.GitHub.bazeliskFile(OS.Linux, CpuArch.X86_64)?.url)
    assertEquals("$releaseUrl/bazelisk-darwin-arm64", BazeliskDownload.GitHub.bazeliskFile(OS.macOS, CpuArch.ARM64)?.url)
    assertEquals("$releaseUrl/bazelisk-windows-amd64.exe", BazeliskDownload.GitHub.bazeliskFile(OS.Windows, CpuArch.X86_64)?.url)
    assertEquals(version, BazeliskDownload.GitHub.bazeliskFile(OS.Linux, CpuArch.X86_64)?.version)
  }

  @Test
  fun `GitHub has no file for an unsupported platform`() {
    assertNull(BazeliskDownload.GitHub.bazeliskFile(OS.FreeBSD, CpuArch.X86_64))
    assertNull(BazeliskDownload.GitHub.bazeliskFile(OS.Linux, CpuArch.X86))
  }

  @Test
  fun `Off forces no download`() {
    assertNull(BazeliskDownload.Off.bazeliskFile(OS.Linux, CpuArch.X86_64))
  }
}
