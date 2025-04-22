package org.jetbrains.bazel.workspacecontext.provider

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.EnvironmentUtil
import org.apache.commons.io.FileUtils.copyURLToFile
import org.apache.logging.log4j.LogManager
import org.jetbrains.bazel.commons.FileUtils
import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractorException
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import java.io.File
import java.net.URI
import java.nio.file.Path

private val log = LogManager.getLogger(BazelBinarySpec::class.java)

// TODO(abrams): update tests for the whole flow and mock different OSes
internal object BazelBinarySpecExtractor : ExecutionContextEntityExtractor<BazelBinarySpec> {
  override fun fromProjectView(projectView: ProjectView): BazelBinarySpec {
    val extracted = projectView.bazelBinary?.value
    return if (extracted != null) {
      BazelBinarySpec(extracted)
    } else {
      val path =
        findBazelOnPathOrNull() ?: downloadBazelisk()
          ?: throw ExecutionContextEntityExtractorException(
            "bazel path",
            "Could not find bazel on your PATH nor download bazelisk",
          )
      BazelBinarySpec(path)
    }
  }

  private fun downloadBazelisk(): Path? {
    log.info("Downloading bazelisk")
    val downloadLink =
      calculateBazeliskDownloadLink()?.let {
        try {
          URI(it).toURL()
        } catch (e: Exception) {
          log.error("Could not parse bazelisk download link: $it")
          return null
        }
      }
    if (downloadLink == null) {
      log.error(
        "Could not calculate bazelisk download link (your OS should be one of: windows-amd64, linux-amd64, linux-arm64, darwin)",
      )
      return null
    }
    val cache = FileUtils.getCacheDirectory("bazelbsp")
    if (cache == null) {
      log.error("Could not find cache directory")
      return null
    }
    // Download bazelisk to the cache folder
    val bazeliskFile = File(cache, "bazelisk")
    if (bazeliskFile.exists()) {
      log.info("Bazelisk already exists in the cache folder: ${bazeliskFile.path}")
    } else {
      log.info("Downloading bazelisk to the cache folder: ${bazeliskFile.path}")
      copyURLToFile(
        downloadLink,
        bazeliskFile,
        60 * 1000,
        60 * 1000,
      )
      log.info("Downloaded bazelisk")
      bazeliskFile.setExecutable(true)
      log.info("Set bazelisk binary to be executable")
    }
    return bazeliskFile.toPath()
  }

  private fun calculateBazeliskDownloadLink(): String? {
    val base = "https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-"
    val isArm = SystemInfo.isAarch64
    val suffix =
      when {
        SystemInfo.isMac -> "darwin"
        SystemInfo.isWindows && !isArm -> "windows-amd64.exe"
        SystemInfo.isWindows && isArm -> "windows-arm64.exe"
        SystemInfo.isLinux && !isArm -> "linux-amd64"
        SystemInfo.isLinux && isArm -> "linux-arm64"
        else -> null
      }
    if (suffix == null) {
      log.error(
        "Could not calculate bazelisk download link (your OS should be one of: windows-amd64, linux-amd64, linux-arm64, linux-aarch64, darwin)",
      )
      return null
    }
    return base + suffix
  }

  private fun findBazelOnPathOrNull(): Path? =
    splitPath()
      .flatMap { listOf(bazelFile(it, "bazel"), bazelFile(it, "bazelisk")) }
      .filterNotNull()
      .firstOrNull()

  private fun splitPath(): List<String> = EnvironmentUtil.getValue("PATH")?.split(File.pathSeparator).orEmpty()

  private fun bazelFile(path: String, executable: String): Path? {
    val file = File(path, calculateExecutableName(executable))
    return if (file.exists() && file.canExecute()) file.toPath() else null
  }

  private fun calculateExecutableName(name: String): String =
    when {
      SystemInfo.isWindows -> "$name.exe"
      else -> name
    }
}
