package org.jetbrains.bazel.languages.starlark.formatting

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.NioFiles
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.testFramework.TestModeFlags
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import com.intellij.util.download.DownloadableFileDescription
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import com.intellij.util.ui.EDT
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path


object BuildifierDownloader {
  private val LOG = Logger.getInstance(BuildifierDownloader::class.java)

  /**
   * Key for [TestModeFlags] to mock the OS version.
   */
  val OS_KEY: Key<OS> = Key<OS>("OS_KEY")

  /**
   * Key for [TestModeFlags] to mock the CpuArch.
   */
  val CPU_ARCH_KEY: Key<CpuArch> = Key<CpuArch>("CPU_ARCH_KEY")

  /**
   * Hardcoded buildifier version, update it to bump the version.
   */
  private const val BUILDIFIER_VERSION = "7.1.2"

  private val DOWNLOAD_PATH: Path = PathManager.getPluginsDir().resolve("buildifier")
  private const val DOWNLOAD_URL = "https://github.com/bazelbuild/buildtools/releases/download"

  fun canDownload(): Boolean {
    return downloadUrl != null
  }

  suspend fun downloadWithProgress(project: Project): Path {
    return withBackgroundProgress(project, "Downloading buildifier") {
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
      val version: String = BUILDIFIER_VERSION.replace('.', '_')

      if (os == OS.Windows) {
        return String.format("buildifier_%s.exe", version)
      } else {
        return String.format("buildifier_%s", version)
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

      // example: https://github.com/bazelbuild/buildtools/releases/download/v7.1.2/buildifier-darwin-amd64
      val url = String.format(
        "%s/v%s/buildifier-%s-%s",
        DOWNLOAD_URL,
        BUILDIFIER_VERSION,
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
