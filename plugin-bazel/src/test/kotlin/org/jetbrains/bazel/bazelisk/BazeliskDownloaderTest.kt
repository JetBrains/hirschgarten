package org.jetbrains.bazel.bazelisk

import com.intellij.testFramework.TestModeFlags
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class BazeliskDownloaderTest {

  @AfterEach
  fun tearDown() {
    TestModeFlags.reset(BazeliskDownloader.OS_KEY)
    TestModeFlags.reset(BazeliskDownloader.CPU_ARCH_KEY)
  }

  @Test
  fun testCanDownloadOnSupportedPlatforms() {
    // Test Windows x86_64
    TestModeFlags.set(BazeliskDownloader.OS_KEY, OS.Windows)
    TestModeFlags.set(BazeliskDownloader.CPU_ARCH_KEY, CpuArch.X86_64)
    assertTrue(BazeliskDownloader.canDownload())

    // Test macOS x86_64
    TestModeFlags.set(BazeliskDownloader.OS_KEY, OS.macOS)
    TestModeFlags.set(BazeliskDownloader.CPU_ARCH_KEY, CpuArch.X86_64)
    assertTrue(BazeliskDownloader.canDownload())

    // Test macOS ARM64
    TestModeFlags.set(BazeliskDownloader.OS_KEY, OS.macOS)
    TestModeFlags.set(BazeliskDownloader.CPU_ARCH_KEY, CpuArch.ARM64)
    assertTrue(BazeliskDownloader.canDownload())

    // Test Linux x86_64
    TestModeFlags.set(BazeliskDownloader.OS_KEY, OS.Linux)
    TestModeFlags.set(BazeliskDownloader.CPU_ARCH_KEY, CpuArch.X86_64)
    assertTrue(BazeliskDownloader.canDownload())

    // Test Linux ARM64
    TestModeFlags.set(BazeliskDownloader.OS_KEY, OS.Linux)
    TestModeFlags.set(BazeliskDownloader.CPU_ARCH_KEY, CpuArch.ARM64)
    assertTrue(BazeliskDownloader.canDownload())
  }

  @Test
  fun testCannotDownloadOnUnsupportedPlatforms() {
    // Test unsupported CPU architecture
    TestModeFlags.set(BazeliskDownloader.OS_KEY, OS.Linux)
    TestModeFlags.set(BazeliskDownloader.CPU_ARCH_KEY, CpuArch.X86)
    assertFalse(BazeliskDownloader.canDownload())

    // Test unsupported OS
    TestModeFlags.set(BazeliskDownloader.OS_KEY, OS.FreeBSD)
    TestModeFlags.set(BazeliskDownloader.CPU_ARCH_KEY, CpuArch.X86_64)
    assertFalse(BazeliskDownloader.canDownload())
  }
}
