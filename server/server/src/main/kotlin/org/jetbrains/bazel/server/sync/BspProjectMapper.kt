package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.Project
import org.jetbrains.bsp.protocol.BspJvmClasspath
import org.jetbrains.bsp.protocol.DirectoryItem
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.RawAspectTarget
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import java.nio.file.Path
import kotlin.io.path.relativeToOrNull

class BspProjectMapper(private val bazelRunner: BazelRunner, private val bspInfo: BspInfo) {
  fun workspaceTargets(project: AspectSyncProject): WorkspaceBuildTargetsResult {
    val targets =
      project.targets
        .mapValues { RawAspectTarget(it.value) }
    return WorkspaceBuildTargetsResult(
      targets = targets,
      rootTargets = project.rootTargets,
      hasError = project.hasError,
    )
  }

  fun workspaceDirectories(project: Project): WorkspaceDirectoriesResult {
    // bazel symlinks exclusion logic is now taken care by BazelSymlinkExcludeService,
    // so there is no need for excluding them here anymore

    val directoriesSection = project.workspaceContext.directories

    val workspaceRoot = project.workspaceRoot

    val additionalDirectoriesToExclude = computeAdditionalDirectoriesToExclude(workspaceRoot)
    val directoriesToExclude = directoriesSection.excludedValues + additionalDirectoriesToExclude

    return WorkspaceDirectoriesResult(
      includedDirectories = directoriesSection.values.map { it.toDirectoryItem() },
      excludedDirectories = directoriesToExclude.map { it.toDirectoryItem() },
    )
  }

  fun workspaceBazelRepoMapping(project: Project): WorkspaceBazelRepoMappingResult = WorkspaceBazelRepoMappingResult(project.repoMapping)

  private fun computeAdditionalDirectoriesToExclude(workspaceRoot: Path): List<Path> =
    listOf(
      bspInfo.bazelBspDir,
      workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY),
    )

  private fun Path.toDirectoryItem() =
    DirectoryItem(
      uri = this.toUri().toString(),
    )

  suspend fun inverseSources(project: AspectSyncProject, inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    val documentRelativePath =
      inverseSourcesParams.textDocument.path
        .relativeToOrNull(project.workspaceRoot) ?: throw RuntimeException("File path outside of project root")
    return InverseSourcesQuery.inverseSourcesQuery(documentRelativePath, bazelRunner, project.bazelRelease, project.workspaceContext)
  }

  suspend fun jvmBuilderParams(project: Project): JvmToolchainInfo =
    JvmToolchainQuery.jvmToolchainQuery(bspInfo, bazelRunner, project.workspaceContext)

  suspend fun classpathQuery(project: Project, target: Label): BspJvmClasspath =
    ClasspathQuery.classPathQuery(target, bspInfo, bazelRunner, project.workspaceContext)
}
