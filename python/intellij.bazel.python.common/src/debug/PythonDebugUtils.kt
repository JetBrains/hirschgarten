package org.jetbrains.bazel.python.debug

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.EnvironmentUtil
import com.jetbrains.python.PythonFileType
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bsp.protocol.PythonBuildTarget
import java.io.File
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.runCatching

internal object PythonDebugUtils {
  data class PythonDebugInfo (
    val pythonFile: Path,
    val environmentVariables: Map<String, String> = emptyMap(),
    val workingDirectory: Path,
  )

  fun preparePythonDebug(project: Project, target: Label): PythonDebugInfo? {
    val pythonTargetData = getPythonTargetData(project, target) ?: return null
    val runnerScript = pythonTargetData.runnerScript ?: return null
    val scriptType = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(runnerScript)?.fileType ?: return null

    val runfiles = pythonTargetData.findRunfilesWorkspaceRoot() ?: return null
    val runfilesRoot = runfiles.parent?.toString().orEmpty()
    val pythonPath = buildPythonPathEnv(runfiles, pythonTargetData.imports, EnvironmentUtil.getValue("PYTHONPATH"))
    val envs = mapOf(
      "BAZEL_TARGET" to target.toString(),
      "BAZEL_WORKSPACE" to "_main",
      "BAZEL_TARGET_NAME" to target.targetName,
      "TEST_SRCDIR" to runfilesRoot,
      "TEST_WORKSPACE" to "_main",
      "PYTHONPATH" to pythonPath,
    )
    val workingDirectory = findWorkingDirectory(runfiles, target)

    val pythonFile =
      when (scriptType) {
        is PythonFileType -> runnerScript
        else -> pythonTargetData.mainFile ?: return null
      }
    return PythonDebugInfo(pythonFile, envs, workingDirectory)
  }

  /** Assembles a new value for PYTHONPATH environment variable, with single paths separated with ':' (UNIX) or ';' (Windows) */
  internal fun buildPythonPathEnv(runfilesWorkspaceRoot: Path, imports: List<String>, existingPythonPath: String? = null): String {
    val importRoots = imports.map { runfilesWorkspaceRoot.resolve(it) }
    val sitePackageRoots = findSitePackagesRoots(runfilesWorkspaceRoot.parent)
    val allRoots = listOf(runfilesWorkspaceRoot) + importRoots + sitePackageRoots
    val newPythonPath = allRoots.distinct().joinToString(File.pathSeparator)
    return if (existingPythonPath.isNullOrBlank()) {
      newPythonPath
    } else {
      newPythonPath + File.pathSeparator + existingPythonPath
    }
  }

  internal fun findWorkingDirectory(runfilesWorkspaceRoot: Path, target: Label): Path {
    val packageWorkingDirectory = runfilesWorkspaceRoot.resolve(target.packagePath.toString())
    return packageWorkingDirectory.takeIf { it.isDirectory() } ?: runfilesWorkspaceRoot
  }

  fun findRealSourceFile(
    project: Project,
    target: Label,
    file: String,
  ): String {
    val rootDir = project.rootDir.toNioPathOrNull() ?: return file
    val filePath = Paths.get(file).toRealPath()
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
    project.targetUtils.getBuildTargetForLabel(target)?.data?.firstNotNullOfOrNull { it as? PythonBuildTarget }

  private fun PythonBuildTarget.findRunfilesWorkspaceRoot(): Path? =
    this.runnerScript?.let { it.parent?.resolve("${it.name}.runfiles")?.resolve("_main") }

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
}

private const val SITE_PACKAGES_MAX_DEPTH = 4
