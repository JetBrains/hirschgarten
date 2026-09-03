package org.jetbrains.bazel.python.debug

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.execution.Platform
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.EnvironmentUtil
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.debugger.PyDebugRunner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.target.getTargetDataForLabel
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.utils.isUnder
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.distinct
import kotlin.collections.plus
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.runCatching

@ApiStatus.Internal
object PythonDebugUtils {
  data class PythonDebugInfo(
    val pythonFile: Path,
    val pythonBinary: Path? = null,
    val environmentVariables: Map<String, String> = emptyMap(),
    val libraryRoots: List<Path> = emptyList(),
    val workingDirectory: Path,
    val targetArgs: List<String> = emptyList(),
  )

  fun preparePythonDebug(project: Project, target: Label): PythonDebugInfo? {
    val pythonTargetData = getPythonTargetData(project, target) ?: return null
    val runnerScript = pythonTargetData.runnerScript ?: return null
    if (!Files.isRegularFile(runnerScript)) return null

    val runfiles = pythonTargetData.findRunfilesWorkspaceRoot() ?: return null
    val runfilesRoot = runfiles.parent
    val pythonPath = buildPythonPathEnv(runfiles, pythonTargetData.imports, EnvironmentUtil.getValue("PYTHONPATH"))
    val pythonBinary = pythonTargetData.findPythonBinary(runfiles, target)
    val projectRoots = listOfNotNull(project.rootDir.toNioPathOrNull(), runfiles)
    val libraryRoots = listOfNotNull(
      runnerScript.takeIf { pythonTargetData.mainFile != runnerScript },
      runfilesRoot,
      findStage2Bootstrap(runfiles, runnerScript),
      pythonBinary?.findRunfilesInterpreterRoot(runfilesRoot),
    )
    val envs = mapOf(
      "BAZEL_TARGET" to target.toString(),
      "BAZEL_WORKSPACE" to "_main",
      "BAZEL_TARGET_NAME" to target.targetName,
      "TEST_SRCDIR" to runfilesRoot?.toString().orEmpty(),
      "TEST_WORKSPACE" to "_main",
      "PYTHONPATH" to pythonPath,
      PyDebugRunner.IDE_PROJECT_ROOTS to projectRoots.toPathList(), // ignore generated Bazel scripts when "Ignore library files" is enabled
    )
    val workingDirectory = findWorkingDirectory(runfiles, target)

    val pythonFile =
      if (runnerScript.isPythonScript()) {
        runnerScript
      } else {
        pythonTargetData.mainFile ?: return null
      }
    return PythonDebugInfo(
      pythonFile = pythonFile,
      pythonBinary = pythonBinary,
      environmentVariables = envs,
      libraryRoots = libraryRoots.distinct(),
      workingDirectory = workingDirectory,
      targetArgs = pythonTargetData.targetArgs,
    )
  }

  private fun PythonBuildTarget.findPythonBinary(runfilesWorkspaceRoot: Path, target: Label): Path? =
    findVenvPythonBinary(runfilesWorkspaceRoot, target) ?: interpreter

  private fun PythonBuildTarget.findVenvPythonBinary(runfilesWorkspaceRoot: Path, target: Label): Path? {
    val runfilesRoot = runfilesWorkspaceRoot.parent ?: return null
    val manifest = runfilesRoot.resolve(RUNFILES_MANIFEST_NAME)
    if (!Files.isRegularFile(manifest)) return null

    val pythonBinaryNames = (listOfNotNull(interpreter?.fileName?.toString()) + DEFAULT_PYTHON_BINARY_NAMES).distinct().toSet()
    return runCatching {
      Files.newBufferedReader(manifest).useLines { lines ->
        lines.firstNotNullOfOrNull { line ->
          val entry = line.toRunfilesManifestEntry() ?: return@firstNotNullOfOrNull null
          if (!entry.runfilesPath.isTargetVenvPythonBinary(target, pythonBinaryNames)) return@firstNotNullOfOrNull null
          val candidate = runfilesRoot.resolve(entry.runfilesPath)
          candidate.takeIf { Files.isExecutable(it) }
        }
      }
    }.getOrNull()
  }

  private fun String.toRunfilesManifestEntry(): RunfilesManifestEntry? {
    if (isBlank()) return null
    val separatorIndex = indexOf(' ')
    return if (separatorIndex < 0) {
      RunfilesManifestEntry(runfilesPath = this, realPath = null)
    } else {
      RunfilesManifestEntry(
        runfilesPath = substring(0, separatorIndex),
        realPath = Paths.get(substring(separatorIndex + 1)),
      )
    }
  }

  private fun String.isTargetVenvPythonBinary(target: Label, pythonBinaryNames: Set<String>): Boolean {
    val packagePath = target.packagePath.toString()
    val pathInWorkspace = removePrefix("$MAIN_WORKSPACE_RUNFILES_PATH/")
    if (pathInWorkspace == this) return false
    val pathInPackage =
      if (packagePath.isEmpty()) {
        pathInWorkspace
      } else {
        pathInWorkspace.removePrefix("$packagePath/")
      }
    if (pathInPackage == pathInWorkspace && packagePath.isNotEmpty()) return false

    val segments = pathInPackage.split('/')
    if (segments.size != 3 || segments[1] != "bin") return false
    val venvName = segments[0].removeSuffix(".venv").removePrefix("_")
    return venvName == target.targetName.removePrefix("_") && segments[2] in pythonBinaryNames
  }

  private fun Path.isPythonScript(): Boolean =
    FileTypeRegistry.getInstance().getFileTypeByFileName(name) is PythonFileType || hasPythonHashBang()

  private fun Path.hasPythonHashBang(): Boolean =
    runCatching {
      Files.newInputStream(this).use { input ->
        val bytes = ByteArray(HASH_BANG_MAX_BYTES)
        val length = input.read(bytes)
        if (length <= 0) {
          return@runCatching false
        }
        val firstChars = String(bytes, 0, length, StandardCharsets.UTF_8)
        val firstLine = firstChars.substringBefore('\n')
        firstLine.startsWith("#!") && firstLine.indexOf(PYTHON_HASH_BANG_MARKER, startIndex = 2) >= 0
      }
    }.getOrDefault(false)

  /** Assembles a new value for PYTHONPATH environment variable, with single paths separated with ':' (UNIX) or ';' (Windows) */
  @VisibleForTesting
  fun buildPythonPathEnv(runfilesWorkspaceRoot: Path, imports: List<String>, existingPythonPath: String? = null): String {
    val importRoots = imports.map { runfilesWorkspaceRoot.resolve(it) }
    val sitePackageRoots = findSitePackagesRoots(runfilesWorkspaceRoot.parent)
    val allRoots =
      buildList {
        addAll(listOf(runfilesWorkspaceRoot))
        addAll(importRoots)
        addAll(sitePackageRoots)
      }

    val pathSeparator = Platform.current().pathSeparator.toString()
    val newPythonPath = allRoots.distinct().joinToString(pathSeparator)
    return if (existingPythonPath.isNullOrBlank()) {
      newPythonPath
    } else {
      newPythonPath + pathSeparator + existingPythonPath
    }
  }

  @VisibleForTesting
  fun findWorkingDirectory(runfilesWorkspaceRoot: Path, target: Label): Path {
    val packageWorkingDirectory = runfilesWorkspaceRoot.resolve(target.packagePath.toString())
    return packageWorkingDirectory.takeIf { it.isDirectory() } ?: runfilesWorkspaceRoot
  }

  fun findRealSourceFile(
    project: Project,
    target: Label,
    file: String,
  ): String? {
    val rootDir = project.rootDir.toNioPathOrNull() ?: return file
    val filePath =
      try {
        // if the file string is not a valid path, or the file does not exist, it should not cause a debugger failure
        // throwing an exception might cause debugger freeze, returning null will simply show "frame not available" in stack trace
        Paths.get(file).toRealPath()
      } catch (_: NoSuchFileException) {
        return null
      } catch (_: InvalidPathException) {
        return null
      }
    if (filePath.isUnder(setOf(rootDir))) {
      // after resolving symlinks, path is inside the project - no need to search elsewhere
      return filePath.toString()
    }

    val runFileSourceRoot = getPythonTargetData(project, target)?.findRunfilesWorkspaceRoot()
    if (runFileSourceRoot != null && filePath.startsWith(runFileSourceRoot)) {
      val fileRelativePath = filePath.subpath(runFileSourceRoot.nameCount, filePath.nameCount).toString()
      return rootDir.resolve(fileRelativePath).toString()
    }

    return file
  }

  private fun getPythonTargetData(project: Project, target: Label): PythonBuildTarget? =
    project.targetStorage.getTargetDataForLabel<PythonBuildTarget>(target)

  private fun PythonBuildTarget.findRunfilesWorkspaceRoot(): Path? =
    this.runnerScript?.let { it.parent?.resolve("${it.name}.runfiles")?.resolve("_main") }

  private fun findStage2Bootstrap(runfilesWorkspaceRoot: Path, runnerScript: Path): Path? =
    runfilesWorkspaceRoot
      .resolve("_${runnerScript.name.removeSuffix(".exe")}_stage2_bootstrap.py")
      .takeIf { Files.isRegularFile(it) }

  private fun Path.findRunfilesInterpreterRoot(runfilesRoot: Path?): Path? {
    if (runfilesRoot == null || !startsWith(runfilesRoot)) return null
    val binaryDirectory = parent ?: return null
    return if (binaryDirectory.name == "bin" || binaryDirectory.name == "Scripts") {
      binaryDirectory.parent
    }
    else {
      binaryDirectory
    }
  }

  private fun List<Path>.toPathList(): String = distinct().joinToString(Platform.current().pathSeparator.toString())

  private fun findSitePackagesRoots(runfilesRoot: Path?): List<Path> {
    if (runfilesRoot == null || !Files.isDirectory(runfilesRoot)) {
      return emptyList()
    }

    return runCatching {
      Files.walk(runfilesRoot, SITE_PACKAGES_MAX_DEPTH, FileVisitOption.FOLLOW_LINKS).use { paths ->
        paths
          .filter { Files.isDirectory(it) && it.fileName?.toString() == "site-packages" }
          .toList()
      }
    }.getOrElse {
      emptyList()
    }
  }

  fun extractPythonTargetArgs(target: TargetIdeInfo): List<String> = target.pyIdeInfo.argsList.toList()

  fun buildPythonDebugBazelArguments(debugFlags: List<String>, additionalBazelParams: String?): List<String> =
    debugFlags + transformProgramArguments(additionalBazelParams)

  fun buildPythonDebugScriptParameters(targetArgs: List<String>, runConfigArguments: String?): String? =
    listOf(ParametersListUtil.join(targetArgs), runConfigArguments)
      .filterNot { it.isNullOrBlank() }
      .joinToString(" ")
      .takeIf { it.isNotEmpty() }

  fun buildPythonLibraryRoots(sdk: Sdk, additionalRoots: List<Path>): String {
    val sdkRoots = sdk.rootProvider.getFiles(OrderRootType.CLASSES).map { it.path }
    val allRoots = sdkRoots + additionalRoots.map { it.toString() }
    return allRoots.distinct().joinToString(Platform.current().pathSeparator.toString())
  }
}

private const val SITE_PACKAGES_MAX_DEPTH = 4
private const val HASH_BANG_MAX_BYTES = 256
private const val PYTHON_HASH_BANG_MARKER = "python"
private const val RUNFILES_MANIFEST_NAME = "MANIFEST"
private const val MAIN_WORKSPACE_RUNFILES_PATH = "_main"
private val DEFAULT_PYTHON_BINARY_NAMES = listOf("python3", "python")

private data class RunfilesManifestEntry(
  val runfilesPath: String,
  val realPath: Path?,
)
