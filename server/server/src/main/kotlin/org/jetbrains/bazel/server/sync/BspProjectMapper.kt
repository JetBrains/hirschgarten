package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.BazelCommand.Build.Companion.log
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.toPath
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
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.EnumSet
import kotlin.io.path.notExists
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
    val workspaceRoot = project.workspaceRoot

    val additionalDirectoriesToExclude = computeAdditionalDirectoriesToExclude(workspaceRoot)
    val (includedDirectories, excludedDirectories) = getProjectDirs(
      workspaceRoot,
      project.workspaceContext.directories,
      project.workspaceContext.targets,
    )

    return WorkspaceDirectoriesResult(
      includedDirectories = includedDirectories.map { it.toDirectoryItem() },
      excludedDirectories = excludedDirectories.map { it.toDirectoryItem() } +
                            additionalDirectoriesToExclude.map { it.toDirectoryItem() },
    )
  }

  private fun getProjectDirs(
    workspaceRoot: Path,
    directories: List<ExcludableValue<Path>>,
    targets: List<ExcludableValue<Label>>
  ): Pair<Set<Path>, Set<Path>> {
    val included : MutableSet<Path> = mutableSetOf()
    val excluded : MutableSet<Path> = mutableSetOf()

    for (directory in directories) {
      val relativeToWorkspaceRoot = workspaceRoot.resolve(directory.value).normalize()
      when (directory) {
        is ExcludableValue.Included<Path> -> included.add(relativeToWorkspaceRoot)
        is ExcludableValue.Excluded<Path> -> excluded.add(relativeToWorkspaceRoot)
      }
    }

    val includedFromTargets : MutableSet<Path> = mutableSetOf()
    val excludedFromTargets : MutableSet<Path> = mutableSetOf()
    // If we include (exclude) a directory by target, we want to add (remove) only directories of it's package
    // and not subpackages, so we need to additionally include (exclude) them.
    val excludedAdditionally : MutableSet<Path> = mutableSetOf()
    val includedAdditionally : MutableSet<Path> = mutableSetOf()

    for (target in targets) {
      val path = workspaceRoot.resolve(target.value.packagePath.toPath()).normalize()
      val subPackages = getSubPackages(path, workspaceRoot)
      val (targetSet, additionalSet) =
        if (target is ExcludableValue.Included) includedFromTargets to excludedAdditionally
        else excludedFromTargets to includedAdditionally

      targetSet.add(path)
      if (target.value.isRecursive) targetSet.addAll(subPackages)
      else additionalSet.addAll(subPackages)
    }

    // We need a hierarchy of collected sets to avoid cases when one directory is both included and excluded:
    // includedAdditionally < excludedAdditionally < included < excluded < includedFromTargets < excludedFromTargets.
    // Note that at this point, if in .bazelproject file deriveTargetsFromDirectories = true, we already have all
    // items from the directories section in targets, so in practice (from the user point of view) the hierarchy is:
    // includedAdditionally < excludedAdditionally < included < includedFromTargets < excluded < excludedFromTargets.
    // When deriveTargetsFromDirectories is true we want to treat directories as if they are included as targets
    // so then, excludes always overwrite includes.
    // Otherwise, targets section items are stronger than directories section items (include form targets overwrites
    // exclude from directories).
    // This behavior is consistent with targets shown in the toolwindow.
    includedAdditionally.removePresentIn(excludedAdditionally, included, excluded, includedFromTargets, excludedFromTargets)
    excludedAdditionally.removePresentIn(included, excluded, includedFromTargets, excludedFromTargets)
    included.removePresentIn(excluded, includedFromTargets, excludedFromTargets)
    excluded.removePresentIn(includedFromTargets, excludedFromTargets)
    includedFromTargets.removePresentIn(excludedFromTargets)

    return (included + includedFromTargets + includedAdditionally to excluded + excludedFromTargets + excludedAdditionally)
  }

  private fun MutableSet<Path>.removePresentIn(vararg higherPrioritySets: Set<Path>) {
    this.removeIf { item ->
      higherPrioritySets.any { set -> item in set }
    }
  }

  private fun getSubPackages(path: Path, workspaceRoot: Path): Set<Path> {
    val packages = mutableSetOf<Path>()
    if (path.notExists()) return packages

    try {
      val options = EnumSet.of(FileVisitOption.FOLLOW_LINKS)
      Files.walkFileTree(path, options, Int.MAX_VALUE, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
          val dirName = dir.fileName.toString()

          if (dirName.startsWith(".") ||
              (dirName.startsWith("bazel-") && Files.isSymbolicLink(dir))) {
            return FileVisitResult.SKIP_SUBTREE
          }
          if (Files.isRegularFile(dir.resolve("BUILD")) ||
              Files.isRegularFile(dir.resolve("BUILD.bazel"))) {
            packages.add(workspaceRoot.resolve(dir).normalize())
          }
          return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
          return FileVisitResult.CONTINUE
        }
      })
    } catch (e: IOException) {
      log.error("Error while walking directory $path", e)
    }
    return packages
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

  suspend fun jvmBuilderParamsForTarget(project: Project, target: Label): JvmToolchainInfo =
    JvmToolchainQuery.jvmToolchainQueryForTarget(bspInfo, bazelRunner, project.workspaceContext, target)

  suspend fun classpathQuery(project: Project, target: Label): BspJvmClasspath =
    ClasspathQuery.classPathQuery(target, bspInfo, bazelRunner, project.workspaceContext)
}
