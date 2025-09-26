package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.io.NioFiles
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.pathString

private val log = logger<ProjectViewToWorkspaceContextConverter>()

/**
 * Converts the new ProjectView to legacy WorkspaceContext for interfacing with the server layer.
 * This is a temporary bridge until the server layer can be updated to use the new ProjectView directly.
 */
object ProjectViewToWorkspaceContextConverter {
  fun convert(
    projectView: ProjectView,
    dotBazelBspDirPath: Path,
    workspaceRoot: Path,
  ): WorkspaceContext {
    val dirs = createDirectoriesFromProjectView(projectView, workspaceRoot)
    val targets =
      if (projectView.deriveTargetsFromDirectories) {
        createTargetsFromDirectories(projectView.targets, projectView.directories)
      } else {
        projectView.targets
      }
    return WorkspaceContext(
      targets = targets,
      directories = dirs,
      buildFlags = projectView.buildFlags,
      syncFlags = projectView.syncFlags,
      debugFlags = projectView.debugFlags,
      bazelBinary = projectView.bazelBinary ?: resolveBazelBinary(),
      allowManualTargetsSync = projectView.allowManualTargetsSync,
      dotBazelBspDirPath = dotBazelBspDirPath,
      importDepth = projectView.importDepth,
      enabledRules = projectView.enabledRules,
      ideJavaHomeOverride = projectView.ideJavaHomeOverride,
      shardSync = projectView.shardSync,
      targetShardSize = projectView.targetShardSize,
      shardingApproach = projectView.shardingApproach,
      importRunConfigurations = projectView.importRunConfigurations,
      gazelleTarget = projectView.gazelleTarget,
      indexAllFilesInDirectories = projectView.indexAllFilesInDirectories,
      pythonCodeGeneratorRuleNames = projectView.pythonCodeGeneratorRuleNames,
      importIjars = projectView.importIjars,
      deriveInstrumentationFilterFromTargets = projectView.deriveInstrumentationFilterFromTargets,
      indexAdditionalFilesInDirectories = projectView.indexAdditionalFilesInDirectories,
    )
  }

  private fun createDirectoriesFromProjectView(projectView: ProjectView, workspaceRoot: Path): List<ExcludableValue<Path>> {
    if (projectView.directories.isEmpty()) {
      // Default to whole project if no directories specified
      return listOf(ExcludableValue.included(workspaceRoot))
    }

    return projectView.directories.map { directory ->
      val relativeToWorkspaceRoot = workspaceRoot.resolve(directory.value).normalize()
      when (directory) {
        is ExcludableValue.Included<Path> -> ExcludableValue.Included(relativeToWorkspaceRoot)
        is ExcludableValue.Excluded<Path> -> ExcludableValue.Excluded(relativeToWorkspaceRoot)
      }
    }
  }

  private fun resolveBazelBinary(): Path {
    // First try to find bazel or bazelisk on PATH
    findBazelOnPathOrNull()?.let { return it }

    // If not found, try to download bazelisk
    return downloadBazelisk() ?: Path.of("bazel")
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

  private fun downloadBazelisk(): Path? {
    val downloadUrl = calculateBazeliskDownloadLink() ?: return null

    // Use IntelliJ's system cache directory
    val cacheDir = Path.of(PathManager.getSystemPath(), "bazel-plugin")
    try {
      Files.createDirectories(cacheDir)
    } catch (e: Exception) {
      log.error("Could not create cache directory", e)
      return null
    }

    // Download bazelisk to the cache folder
    val bazeliskFile = cacheDir.resolve("bazelisk")
    if (Files.exists(bazeliskFile)) {
      log.info("Bazelisk already exists in the cache folder: ${bazeliskFile.toAbsolutePath()}")
      return bazeliskFile
    }

    log.info("Downloading bazelisk to the cache folder: ${bazeliskFile.toAbsolutePath()}")

    // Download with progress indicator
    try {
      val task =
        object : Task.Modal(null, BazelPluginBundle.message("bazel.project.downloading.bazelisk"), true) {
          override fun run(indicator: ProgressIndicator) {
            indicator.text = BazelPluginBundle.message("bazel.project.downloading.bazelisk.binary")
            indicator.isIndeterminate = false

            HttpRequests.request(downloadUrl).saveToFile(bazeliskFile, indicator)

            indicator.text = BazelPluginBundle.message("bazel.project.downloading.bazelisk.setting_permissions")
            NioFiles.setExecutable(bazeliskFile)
          }
        }

      ProgressManager.getInstance().run(task)
      log.info("Downloaded bazelisk successfully")
      return bazeliskFile
    } catch (e: Exception) {
      log.error("Failed to download bazelisk", e)
      Files.deleteIfExists(bazeliskFile)
      return null
    }
  }

  private fun calculateBazeliskDownloadLink(): String? {
    val base = "https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-"

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

  private fun createTargetsFromDirectories(
    targets: List<ExcludableValue<Label>>,
    dirs: List<ExcludableValue<Path>>,
  ): List<ExcludableValue<Label>> {
    fun hasEmptyIncludedAndEmptyExcluded(list: List<ExcludableValue<*>>): Boolean =
      list.none { it.isIncluded() } && list.none { !it.isIncluded() }

    fun hasEmptyIncludedAndNonEmptyExcluded(list: List<ExcludableValue<*>>): Boolean =
      list.none { it.isIncluded() } && list.any { !it.isIncluded() }

    fun mapDirectoryToTarget(buildDirectoryIdentifier: Path): Label =
      if (buildDirectoryIdentifier.pathString == ".") {
        Label.parse("//...")
      } else {
        Label.parse("//" + buildDirectoryIdentifier.pathString + "/...")
      }

    when {
      dirs.isEmpty() -> return targets
      hasEmptyIncludedAndEmptyExcluded(dirs) -> return targets
      hasEmptyIncludedAndNonEmptyExcluded(dirs) -> {
        throw IllegalArgumentException("'directories' section has no included targets.")
      }

      else -> {
        val directoriesValues =
          dirs
            .filter { it.isIncluded() }
            .map { ExcludableValue.included(mapDirectoryToTarget(it.value)) }
        val directoriesExcludedValues =
          dirs
            .filter { !it.isIncluded() }
            .map { ExcludableValue.excluded(mapDirectoryToTarget(it.value)) }
        return targets + directoriesValues + directoriesExcludedValues
      }
    }
  }
}
