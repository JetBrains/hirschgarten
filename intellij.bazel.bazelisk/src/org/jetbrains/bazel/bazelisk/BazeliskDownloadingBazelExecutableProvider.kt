package org.jetbrains.bazel.bazelisk

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.NioFiles
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService
import org.jetbrains.bazel.sync.environment.BazelProjectContextService
import org.jetbrains.bazel.sync.environment.getProjectRootDirOrThrow
import org.jetbrains.bazel.workspace.BazelExecutableProvider
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.readText

internal class BazeliskDownloadingBazelExecutableProvider : BazelExecutableProvider {
  companion object {
    private val log = logger<BazeliskDownloadingBazelExecutableProvider>()
  }

  override suspend fun computeBazelExecutable(project: Project): Path? {
    val workspaceRoot = project.service<BazelProjectContextService>()
      .getProjectRootDirOrThrow().toNioPath()
    val bazeliskVersion = readBazeliskVersion(workspaceRoot)
    if (bazeliskVersion != null) {
      // If requested a specific version, download it
      return downloadBazelisk(bazeliskVersion) ?: throw IllegalStateException("Failed to download bazelisk $bazeliskVersion")
    }

    if (!service<BazelApplicationContextService>().forceBazeliskDownload) {
      // Otherwise try to find bazel or bazelisk on PATH
      findBazelOnPathOrNull()?.let { return it }
    }

    // If not found, try to download any version of bazelisk
    return downloadBazelisk(bazeliskVersion = null) ?: Path.of("bazel")
  }

  /**
   * Logic specific to IntelliJ monorepo, see https://youtrack.jetbrains.com/issue/BAZEL-2573/Hermetic-workflow-.bazeliskversion
   */
  private fun readBazeliskVersion(workspaceRoot: Path): String? {
    val bazeliskVersionFile = workspaceRoot.resolve(Constants.BAZELISK_VERSION_FILE_NAME)
    return runCatching { bazeliskVersionFile.readText() }
      .getOrNull()
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
  }

  private fun findBazelOnPathOrNull(): Path? =
    splitPath()
      .flatMap { listOf(bazelFile(it, "bazel"), bazelFile(it, "bazelisk")) }
      .filterNotNull()
      .firstOrNull()

  private fun splitPath(): List<String> = System.getenv("PATH")?.split(File.pathSeparator).orEmpty()

  private fun bazelFile(pathStr: String, executable: String): Path? {
    val executableName = if (OS.CURRENT == OS.Windows) "$executable.exe" else executable
    val path = Path.of(pathStr, executableName)
    return if (path.exists() && path.isExecutable()) path else null
  }

  private fun downloadBazelisk(bazeliskVersion: String?): Path? {
    val downloadUrl = calculateBazeliskDownloadLink(bazeliskVersion) ?: return null

    // Use IntelliJ's system cache directory
    val cacheDir = Path.of(PathManager.getSystemPath(), "bazel-plugin")
    try {
      Files.createDirectories(cacheDir)
    }
    catch (e: Exception) {
      log.error("Could not create cache directory", e)
      return null
    }

    // Download bazelisk to the cache folder
    val targetFileName = if (bazeliskVersion == null) "bazelisk" else "bazelisk-$bazeliskVersion"
    val bazeliskFile = cacheDir.resolve(targetFileName)
    if (Files.exists(bazeliskFile)) {
      log.info("Bazelisk already exists in the cache folder: ${bazeliskFile.toAbsolutePath()}")
      return bazeliskFile
    }

    log.info("Downloading bazelisk to the cache folder: ${bazeliskFile.toAbsolutePath()}")

    // Download with progress indicator
    try {
      val task =
        object : Task.WithResult<Unit, IOException>(null, BazelBazeliskBundle.message("bazel.project.downloading.bazelisk"), true) {
          override fun compute(indicator: ProgressIndicator) {
            indicator.text = BazelBazeliskBundle.message("bazel.project.downloading.bazelisk.binary")
            indicator.isIndeterminate = false

            HttpRequests.request(downloadUrl).saveToFile(bazeliskFile, indicator)

            indicator.text = BazelBazeliskBundle.message("bazel.project.downloading.bazelisk.setting_permissions")
            NioFiles.setExecutable(bazeliskFile)
          }
        }

      ProgressManager.getInstance().run(task)
      log.info("Downloaded bazelisk successfully")
      return bazeliskFile
    }
    catch (e: Exception) {
      log.error("Failed to download bazelisk", e)
      Files.deleteIfExists(bazeliskFile)
      return null
    }
  }

  private fun calculateBazeliskDownloadLink(bazeliskVersion: String?): String? {
    val downloadSegment = if (bazeliskVersion != null) {
      "download/v$bazeliskVersion"
    }
    else {
      "latest/download"
    }
    val base = "https://github.com/bazelbuild/bazelisk/releases/$downloadSegment/bazelisk-"

    val platform =
      when (OS.CURRENT) {
        OS.Windows -> "windows"
        OS.macOS -> "darwin"
        OS.Linux -> "linux"
        else -> null
      }

    val arch =
      when (CpuArch.CURRENT) {
        CpuArch.X86_64 -> "amd64"
        CpuArch.ARM64 -> "arm64"
        else -> null
      }

    if (platform == null || arch == null) {
      log.error("Unsupported platform or architecture for bazelisk download")
      return null
    }

    val suffix = "$platform-$arch"
    return "$base$suffix".takeIf { OS.CURRENT != OS.Windows } ?: "$base$suffix.exe"
  }
}
