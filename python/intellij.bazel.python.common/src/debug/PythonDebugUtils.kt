package org.jetbrains.bazel.python.debug

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.toNioPathOrNull
import com.jetbrains.python.PythonFileType
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bsp.protocol.PythonBuildTarget
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name

internal object PythonDebugUtils {
  data class PythonDebugInfo (
    val pythonFile: Path,
    val environmentVariables: Map<String, String> = emptyMap(),
  )

  fun preparePythonDebug(project: Project, target: Label): PythonDebugInfo? {
    val pythonTargetData = getPythonTargetData(project, target) ?: return null
    val runnerScript = pythonTargetData.runnerScript ?: return null
    val scriptType = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(runnerScript)?.fileType ?: return null

    if (scriptType is PythonFileType) {
      return PythonDebugInfo(runnerScript)
    } else {
      val pythonMain = pythonTargetData.mainFile ?: return null
      val runfiles = pythonTargetData.findRunFileRoot() ?: return null
      val envs = mapOf(
        "BAZEL_TARGET" to target.toString(),
        "BAZEL_WORKSPACE" to "_main",
        "BAZEL_TARGET_NAME" to target.targetName,
        "PYTHONPATH" to runfiles.toString(),
      )
      return PythonDebugInfo(pythonMain, envs)
    }
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

    val runFileSourceRoot = getPythonTargetData(project, target)?.findRunFileRoot()
    if (runFileSourceRoot != null && filePath.startsWith(runFileSourceRoot)) {
      val fileRelativePath = filePath.subpath(runFileSourceRoot.nameCount, filePath.nameCount).toString()
      return rootDir.resolve(fileRelativePath).toString()
    }

    return file
  }

  private fun getPythonTargetData(project: Project, target: Label): PythonBuildTarget? =
    project.targetUtils.getBuildTargetForLabel(target)?.data?.firstNotNullOfOrNull { it as? PythonBuildTarget }

  private fun PythonBuildTarget.findRunFileRoot(): Path? =
    this.runnerScript?.let { it.parent?.resolve("${it.name}.runfiles")?.resolve("_main") }
}
