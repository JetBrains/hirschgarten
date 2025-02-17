package org.jetbrains.bazel.python

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.flow.sync.BazelBinPathService
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.text.drop
import kotlin.text.indexOf

object PythonDebugUtils {
  fun guessRunScriptName(project: Project, targetId: BuildTargetIdentifier): Path {
    val bazelBinPath = getBazelBinPath(project)
    val splitTargetId = targetId.dropRepo().split('/', ':').toTypedArray()
    return Paths.get(bazelBinPath, *splitTargetId)
  }

  fun findRealSourceFile(
    project: Project,
    targetId: BuildTargetIdentifier,
    filePath: String,
  ): String {
    val bazelBin = getBazelBinPath(project)
    val splitTargetId = targetId.dropRepo().split('/', ':')
    val runFilesPathElements = splitTargetId.let { it.dropLast(1) + "${it.last()}.runfiles" + "_main" }.toTypedArray()
    val runFilesPath = Paths.get(bazelBin, *runFilesPathElements).toString()
    return if (filePath.startsWith(runFilesPath)) {
      val basePath = project.basePath ?: error(BazelPluginBundle.message("project.base.path.not.found"))
      filePath.replaceFirst(runFilesPath, basePath)
    } else {
      filePath
    }
  }

  private fun getBazelBinPath(project: Project): String? =
    BazelBinPathService.getInstance(project).bazelBinPath ?: error(BazelPluginBundle.message("bazel.bin.not.found"))

  private fun BuildTargetIdentifier.dropRepo(): String {
    val doubleSlashPosition = this.uri.indexOf("//")
    return this.uri.drop(doubleSlashPosition + 2)
  }
}
