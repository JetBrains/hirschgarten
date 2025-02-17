package org.jetbrains.bazel.python

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.flow.sync.BazelBinPathService
import java.nio.file.Path
import java.nio.file.Paths

object PythonDebugUtils {
  fun guessRunScriptName(project: Project, target: Label): Path {
    val bazelBinPath = getBazelBinPath(project)
    val targetPackage = target.packagePath.pathSegments.toTypedArray()
    return Paths.get(bazelBinPath, *targetPackage, target.targetName)
  }

  fun findRealSourceFile(
    project: Project,
    target: Label,
    filePath: String,
  ): String {
    val bazelBin = getBazelBinPath(project)
    val targetPackage = target.packagePath.pathSegments.toTypedArray()
    val runFilesPath = Paths.get(bazelBin, *targetPackage, "${target.targetName}.runfiles", "_main").toString()
    return if (filePath.startsWith(runFilesPath)) {
      val basePath = project.basePath ?: error(BazelPluginBundle.message("project.base.path.not.found"))
      filePath.replaceFirst(runFilesPath, basePath)
    } else {
      filePath
    }
  }

  private fun getBazelBinPath(project: Project): String? =
    BazelBinPathService.getInstance(project).bazelBinPath ?: error(BazelPluginBundle.message("bazel.bin.not.found"))
}
