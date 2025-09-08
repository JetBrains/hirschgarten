package org.jetbrains.bazel.languages.projectview

import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path

/**
 * Converts the new ProjectView to legacy WorkspaceContext for interfacing with the server layer.
 * This is a temporary bridge until the server layer can be updated to use the new ProjectView directly.
 */
object ProjectViewToWorkspaceContextConverter {
  
  fun convert(
    projectView: ProjectView, 
    dotBazelBspDirPath: Path, 
    workspaceRoot: Path,
    projectViewPath: Path
  ): WorkspaceContext {
    return WorkspaceContext(
      targets = projectView.targets,
      directories = createDirectoriesFromProjectView(projectView, workspaceRoot),
      buildFlags = projectView.buildFlags,
      syncFlags = projectView.syncFlags,
      debugFlags = projectView.debugFlags,
      bazelBinary = projectView.bazelBinary ?: resolveBazelFromPath(),
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
    )
  }
  
  private fun createDirectoriesFromProjectView(projectView: ProjectView, workspaceRoot: Path): List<ExcludableValue<Path>> {
    if (projectView.directories.isEmpty()) {
      // Default to whole project if no directories specified
      return listOf(ExcludableValue.included(workspaceRoot))
    }
    
    return projectView.directories
  }
  
  private fun resolveBazelFromPath(): Path {
    // Simple implementation - look for bazel in PATH
    val pathDirs = System.getenv("PATH")?.split(System.getProperty("path.separator")) ?: emptyList()
    for (dir in pathDirs) {
      val bazelPath = Path.of(dir, "bazel")
      if (bazelPath.toFile().exists() && bazelPath.toFile().canExecute()) {
        return bazelPath
      }
    }
    // Fallback to just "bazel" and let the system resolve it
    return Path.of("bazel")
  }
}