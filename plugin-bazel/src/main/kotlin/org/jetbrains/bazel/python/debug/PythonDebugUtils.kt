package org.jetbrains.bazel.python.debug

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.flow.sync.BazelBinPathService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.runfiles.RunfilesUtils
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
    file: String,
  ): String {
    val bazelBin = getBazelBinPath(project)
    val filePath = Paths.get(file)
    val possibleRunFileLocations =
      guessPossibleWorkspaceNames(bazelBin)
        .map { RunfilesUtils.calculateTargetRunfiles(bazelBin, target).resolve(it) }

    for (runFileLocation in possibleRunFileLocations) {
      if (filePath.startsWith(runFileLocation)) {
        val basePath = project.basePath ?: error(BazelPluginBundle.message("project.base.path.not.found"))
        val fileRelativePath = filePath.subpath(runFileLocation.nameCount, filePath.nameCount).toString()
        return Paths.get(basePath, fileRelativePath).toString()
      }
    }
    return file // none of the runfile locations worked, it's probably an external source (that is expected, it's not a failure)
  }

  private fun getBazelBinPath(project: Project): String =
    BazelBinPathService.getInstance(project).bazelBinPath ?: error(BazelPluginBundle.message("bazel.bin.not.found"))

  // TODO: BAZEL-1836

  /**
   * This function tries to guess the workspace name (one defined in the `WORKSPACE` file if one exists).
   * Bazel runfile path contains this name in it,
   * as mentioned in [Bazel documentation](https://bazel.build/rules/lib/globals/workspace#workspace):
   *
   * 1. `"_main"` is the workspace name in all projects with bzlmod enabled
   * (as per [WorkspaceNameFunction in Bazel code](https://github.com/bazelbuild/bazel/blob/3dcf191b86975577b1643b572d24b0ecebf5bef7/src/main/java/com/google/devtools/build/lib/skyframe/WorkspaceNameFunction.java#L50))
   * 2. `bazel-out` path usually contains the workspace name
   */
  private fun guessPossibleWorkspaceNames(bazelBinPath: String): Sequence<String> =
    sequence {
      yield("_main") // lazy sequence is being used, so in many cases nothing else will be calculated
      val fromBazelBin =
        "execroot.(?<workspace>.*).bazel-out"
          .toRegex()
          .find(bazelBinPath)
          ?.groups
          ?.get(1)
          ?.value
      fromBazelBin?.let { yield(it) }
    }
}
