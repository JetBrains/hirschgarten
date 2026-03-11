package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.environment.getProjectRootDirOrThrow
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path
import kotlin.io.path.pathString

@ApiStatus.Internal
object ProjectViewToWorkspaceContextConverter {
  fun convert(project: Project, projectView: ProjectView, bazelExecutable: Path): WorkspaceContext {
    val workspaceRoot = project.projectCtx.getProjectRootDirOrThrow().toNioPath()
    val dirs = createDirectoriesFromProjectView(projectView, workspaceRoot)
    val targets =
      if (projectView.deriveTargetsFromDirectories) {
        createTargetsFromDirectories(projectView.targets, projectView.directories)
      }
      else {
        projectView.targets
      }
    return WorkspaceContext(
      targets = targets,
      directories = dirs,
      buildFlags = projectView.buildFlags,
      syncFlags = projectView.syncFlags,
      debugFlags = getAllDebugFlags(projectView),
      bazelBinary = projectView.bazelBinary?.let { workspaceRoot.resolve(it) }
                    ?: bazelExecutable,
      allowManualTargetsSync = projectView.allowManualTargetsSync,
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
      preferClassJarsOverSourcelessJars = projectView.preferClassJarsOverSourcelessJars,
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
      }
      else {
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

  private fun getAllDebugFlags(projectView: ProjectView): List<String> =
    (
      projectView.debugFlags +
      (
        projectView.pythonDebugFlags.takeIf {
          // FIXME: this is bad, why project view is aware that it has some python stuff within it?
          //  this defeats purpose of having extensible project view sections...
          BazelFeatureFlags.isPythonSupportEnabled
        } ?: emptyList()
      )
    ).distinct()
}
