package org.jetbrains.bazel.sync.environment

import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.jetbrains.annotations.ApiStatus

/**
 * The forced Bazelisk download of [BazelApplicationContextService.forcedBazeliskDownload].
 *
 * A forced download skips the lookup of Bazel on `PATH`.
 * [Off] forces no download: the plugin looks for Bazel on `PATH` first, and downloads Bazelisk from [GitHub] only when the lookup finds nothing.
 */
@ApiStatus.Internal
interface BazeliskDownload {
  /** Returns the Bazelisk file for [os] and [arch], or `null` to look for Bazel on `PATH` first. */
  fun bazeliskFile(os: OS, arch: CpuArch): BazeliskFile?

  /** A Bazelisk file to download: the Bazelisk [version], the download [url], and the SHA-256 checksum [sha256] of the file. */
  @ApiStatus.Internal
  data class BazeliskFile(val version: String, val url: String, val sha256: String)

  /** Forces no download. */
  object Off : BazeliskDownload {
    override fun bazeliskFile(os: OS, arch: CpuArch): BazeliskFile? = null
  }

  /**
   * Forces the download of Bazelisk [VERSION] from the GitHub releases.
   * When you change [VERSION], also change the checksums.
   * The plugin keeps the checksums in its code, because a checksum from GitHub gives no protection against a changed GitHub release.
   */
  object GitHub : BazeliskDownload {
    const val VERSION: String = "1.30.0"

    private val SHA256: Map<String, String> = mapOf(
      "bazelisk-darwin-amd64" to "a0e6468b0c6b812f782d8cbfb05824121f6980c0bd70aaa1774fb8272bb7b2ad",
      "bazelisk-darwin-arm64" to "555d2ff912ccd336d0060bdb5251ef2f62629c6f8fbadb59005a45d5625793ec",
      "bazelisk-linux-amd64" to "a248ae2b2b62b5baebb748f6fba7203bff1a27e5d01b55399a8406744cb539e8",
      "bazelisk-linux-arm64" to "b2bcc040c213b44586b121bda6d983c35df121b917c3a7a66fb16f2fa0df1a4f",
      "bazelisk-windows-amd64.exe" to "5d14a31e9d8a363c48ba1900dbd56764d730835232eb0d113cce255856ffe3ef",
      "bazelisk-windows-arm64.exe" to "cb832b1d157e0dc5f89108da426f9c6fe087f5b15873e072480ec49cc5afe488",
    )

    override fun bazeliskFile(os: OS, arch: CpuArch): BazeliskFile? {
      val assetName = assetName(os, arch) ?: return null
      val sha256 = SHA256[assetName] ?: return null
      return BazeliskFile(VERSION, "https://github.com/bazelbuild/bazelisk/releases/download/v$VERSION/$assetName", sha256)
    }

    private fun assetName(os: OS, arch: CpuArch): String? {
      val platform = when (os) {
        OS.Windows -> "windows"
        OS.macOS -> "darwin"
        OS.Linux -> "linux"
        else -> return null
      }
      val archName = when (arch) {
        CpuArch.X86_64 -> "amd64"
        CpuArch.ARM64 -> "arm64"
        else -> return null
      }
      val name = "bazelisk-$platform-$archName"
      return if (os == OS.Windows) "$name.exe" else name
    }
  }
}
