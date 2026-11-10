package org.jetbrains.bazel.bazelisk

import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.NioFiles
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.sha256Hex
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.BazelEnvironmentService
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService
import org.jetbrains.bazel.sync.environment.BazeliskDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.workspace.BazelExecutableProvider
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.time.Duration.Companion.seconds

private val log = logger<BazeliskDownloadingBazelExecutableProvider>()
private const val MAX_DOWNLOAD_ATTEMPTS = 3
private val RETRY_BACKOFF = 2.seconds

/**
 * Finds Bazel on `PATH`, or downloads Bazelisk.
 *
 * [BazelApplicationContextService.forcedBazeliskDownload] can force the download and skip the lookup on `PATH`.
 * The plugin keeps the download in `<system>/bazel-plugin/bazelisk-<version>`.
 */
internal class BazeliskDownloadingBazelExecutableProvider : BazelExecutableProvider {
  override suspend fun computeBazelExecutable(project: Project): Path? {
    val forcedFile = service<BazelApplicationContextService>().forcedBazeliskDownload.bazeliskFile(OS.CURRENT, CpuArch.CURRENT)
    if (forcedFile == null) {
      val inPath = BazelEnvironmentService.getInstance(project).findInPath("bazel", "bazelisk")
      if (inPath != null)
        return inPath
    }
    val bazeliskFile = forcedFile ?: BazeliskDownload.GitHub.bazeliskFile(OS.CURRENT, CpuArch.CURRENT) ?: run {
      log.error("Unsupported platform or architecture for bazelisk download")
      return null
    }
    return withContext(Dispatchers.IO) {
      downloadBazelisk(project, bazeliskFile)
    }
  }

  @Suppress("BlockingMethodInNonBlockingContext")
  private suspend fun downloadBazelisk(project: Project, file: BazeliskDownload.BazeliskFile): Path? {
    val expectedSha256 = file.sha256
    val downloadUrl = file.url

    // Use IntelliJ's system cache directory
    val cacheDir = Path.of(PathManager.getSystemPath(), "bazel-plugin")
    try {
      Files.createDirectories(cacheDir)
    }
    catch (e: Exception) {
      log.error("Could not create cache directory", e)
      return null
    }

    val bazeliskFile = cacheDir.resolve("bazelisk-${file.version}")
    if (Files.exists(bazeliskFile)) {
      try {
        checkFileSha256Checksum(bazeliskFile, expectedSha256)
        log.trace("Bazelisk already exists in the cache folder: ${bazeliskFile.toAbsolutePath()}")
        return bazeliskFile
      }
      catch (e: Exception) {
        log.warn("Bazelisk in the cache folder failed the checksum check, downloading it again", e)
      }
    }

    log.info("Downloading Bazelisk to the cache folder: ${bazeliskFile.toAbsolutePath()}")

    // Each download has its own file. The plugin moves the download to bazeliskFile only after the checksum check.
    val downloadFile = try {
      Files.createTempFile(cacheDir, "${bazeliskFile.fileName}-", ".download")
    }
    catch (e: IOException) {
      log.error("Could not create a temporary file for the Bazelisk download", e)
      return null
    }
    try {
      withBackgroundProgress(project, BazelBazeliskBundle.message("bazel.project.downloading.bazelisk")) {
        reportSequentialProgress { reporter ->
          reporter.nextStep(endFraction = 95, BazelBazeliskBundle.message("bazel.project.downloading.bazelisk.binary")) {
            downloadWithRetry(downloadUrl, downloadFile)
          }
          reporter.nextStep(endFraction = 100, BazelBazeliskBundle.message("bazel.project.downloading.bazelisk.setting_permissions")) {
            checkFileSha256Checksum(downloadFile, expectedSha256)
            NioFiles.setExecutable(downloadFile)
            Files.move(downloadFile, bazeliskFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
          }
        }
      }
      log.info("Downloaded bazelisk successfully")
      return bazeliskFile
    }
    catch (e: Throwable) {
      Files.deleteIfExists(downloadFile)
      rethrowControlFlowException(e)
      log.error("Failed to download bazelisk", e)
      return null
    }
  }

  private suspend fun downloadWithRetry(downloadUrl: String, bazeliskFile: Path) {
    var attempt = 1
    while (true) {
      try {
        coroutineToIndicator { indicator ->
          HttpRequests.request(downloadUrl).saveToFile(bazeliskFile, indicator)
        }
        return
      }
      catch (e: IOException) {
        if (attempt >= MAX_DOWNLOAD_ATTEMPTS) throw e
        log.warn("Attempt $attempt of $MAX_DOWNLOAD_ATTEMPTS to download bazelisk from $downloadUrl failed, retrying", e)
        Files.deleteIfExists(bazeliskFile)
        delay(RETRY_BACKOFF * attempt)
        attempt++
      }
    }
  }
}

/**
 * Throws an [IOException] when the SHA-256 checksum of [file] is not [expectedSha256].
 */
@ApiStatus.Internal
fun checkFileSha256Checksum(file: Path, expectedSha256: String) {
  val actualSha256 = sha256Hex(file)
  if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
    throw IOException(BazelBazeliskBundle.message("bazel.bazelisk.checksum.mismatch", file, actualSha256, expectedSha256))
  }
}
