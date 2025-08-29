package org.jetbrains.bazel.bazelisk

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.NioFiles
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.testFramework.TestModeFlags
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.jetbrains.annotations.VisibleForTesting
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object BazeliskDownloader {
  private val LOG = Logger.getInstance(BazeliskDownloader::class.java)

  /**
   * Key for [TestModeFlags] to mock the OS version.
   */
  val OS_KEY: Key<OS> = Key<OS>("BAZELISK_OS_KEY")

  /**
   * Key for [TestModeFlags] to mock the CpuArch.
   */
  val CPU_ARCH_KEY: Key<CpuArch> = Key<CpuArch>("BAZELISK_CPU_ARCH_KEY")

  /**
   * Hardcoded bazelisk version, update it to bump the version.
   */
  private const val BAZELISK_VERSION = "1.27.0"

  private val DOWNLOAD_PATH: Path = PathManager.getPluginsDir().resolve("bazelisk")
  private const val DOWNLOAD_URL = "https://github.com/bazelbuild/bazelisk/releases/download"

  fun canDownload(): Boolean {
    return downloadUrl != null
  }

  suspend fun downloadWithProgress(project: Project): Path {
    return withBackgroundProgress(project, "Downloading bazelisk") {
      download()
    }
  }

  suspend fun download(): Path {
    if (!Files.exists(DOWNLOAD_PATH)) {
      Files.createDirectories(DOWNLOAD_PATH)
    }

    val fileName = fileName
    val file = DOWNLOAD_PATH.resolve(fileName)
    Files.deleteIfExists(file)

    val url = downloadUrl ?: throw IOException("cannot create download url")

    coroutineToIndicator {
      HttpRequests.request(url).saveToFile(file, it)
    }
    NioFiles.setExecutable(file)

    return file
  }

  private val fileName: String
    get() {
      val version = BAZELISK_VERSION.replace('.', '_')

      if (os == OS.Windows) {
        return "bazelisk_${version}.exe"
      } else {
        return "bazelisk_$version"
      }
    }

  private val os: OS
    get() = TestModeFlags.get<OS>(OS_KEY) ?: OS.CURRENT

  private val cpuArch: CpuArch = TestModeFlags.get<CpuArch>(CPU_ARCH_KEY) ?: CpuArch.CURRENT

  private val platformIdentifier: String?
    get() = when (os) {
      OS.Windows -> "windows"
      OS.macOS -> "darwin"
      OS.Linux -> "linux"
      else -> null
    }

  private val architectureIdentifier: String?
    get() = when (cpuArch) {
      CpuArch.X86_64 -> "amd64"
      CpuArch.ARM64 -> "arm64"
      else -> null
    }

  private val downloadUrl: String?
    get() {
      val platform = platformIdentifier ?: return null
      val arch = architectureIdentifier ?: return null

      // example: https://github.com/bazelbuild/bazelisk/releases/download/v1.20.0/bazelisk-darwin-amd64
      val url = String.format(
        "%s/v%s/bazelisk-%s-%s",
        DOWNLOAD_URL,
        BAZELISK_VERSION,
        platform,
        arch,
      )

      return if (os == OS.Windows) {
        "$url.exe"
      } else {
        url
      }
    }
}
