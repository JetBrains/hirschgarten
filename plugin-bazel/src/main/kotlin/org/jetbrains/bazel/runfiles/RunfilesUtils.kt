package org.jetbrains.bazel.runfiles

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.flow.sync.bazelPaths.BazelBinPathService
import org.jetbrains.bazel.label.Label
import java.nio.file.Path
import java.nio.file.Paths

object RunfilesUtils {
  fun calculateTargetRunfiles(project: Project, targetLabel: Label): Path = calculateTargetRunfiles(getBazelBinPath(project), targetLabel)

  fun calculateTargetRunfiles(bazelBin: String, targetLabel: Label): Path =
    Paths.get(bazelBin, *targetLabel.packagePath.pathSegments.toTypedArray(), "${targetLabel.targetName}.runfiles")

  private fun getBazelBinPath(project: Project): String =
    BazelBinPathService.getInstance(project).bazelBinPath ?: error(BazelPluginBundle.message("bazel.bin.not.found"))
}
