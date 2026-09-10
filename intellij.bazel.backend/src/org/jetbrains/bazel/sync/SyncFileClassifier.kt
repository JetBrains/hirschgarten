package org.jetbrains.bazel.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.AllPackagesBeneath
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.PackageType
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.label.TargetType
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.relativeToOrNull

@ApiStatus.Internal
data class SyncFile(
  val path: Path,
  val kind: SyncFileKind,
  val isWorkspaceFile: Boolean = true,
)

@ApiStatus.Internal
sealed interface SyncFileKind {
  data class Build(val pattern: Label) : SyncFileKind

  // `pattern` matches every rule target of every package under the directory
  data class Folder(val pattern: Label) : SyncFileKind

  data class Module(val canonicalRepoName: String) : SyncFileKind
  data object Workspace : SyncFileKind
  data object ProjectView : SyncFileKind

  // `label` is file label, NOT owning target label - you need query for that
  data class Source(val label: Label) : SyncFileKind
  data class Starlark(val label: Label?) : SyncFileKind

  data object BazelDotFile : SyncFileKind
  data object External : SyncFileKind
}

private const val MAIN_CANONICAL_REPO_NAME = ""

private val WORKSPACE_FILE_NAMES: Set<String> = Constants.WORKSPACE_FILE_NAMES.toSet() - Constants.MODULE_BAZEL_FILE_NAME

private val MODULE_FILE_NAMES: Set<String> =
  setOf(Constants.MODULE_BAZEL_FILE_NAME, Constants.MODULE_BAZEL_LOCK_FILE_NAME, Constants.REPO_BAZEL_FILE_NAME)

private val BAZEL_DOT_FILE_NAMES: Set<String> =
  setOf(
    Constants.BAZEL_RC_FILE_NAME,
    Constants.BAZEL_IGNORE_FILE_NAME,
    Constants.BAZEL_VERSION_FILE_NAME,
    Constants.BAZELISK_RC_FILE_NAME,
    Constants.BAZELISK_VERSION_FILE_NAME,
  )

@ApiStatus.Internal
class SyncFileClassifier(private val repoMapping: RepoMapping, private val bazelInfo: BazelInfo) {
  private val localRepositories: Map<String, Path> = when (repoMapping) {
                                                       is BzlmodRepoMapping -> repoMapping.canonicalRepoNameToPath
                                                         .filterKeys { canonicalRepoName -> canonicalRepoName !in repoMapping.nonLocalCanonicalRepoNames }
                                                         .mapValues { (_, repoPath) ->
                                                           bazelInfo.workspaceRoot.resolve(repoPath).normalize()
                                                         }

                                                       RepoMappingDisabled -> mapOf()
                                                     } + mapOf(MAIN_CANONICAL_REPO_NAME to bazelInfo.workspaceRoot.normalize())

  fun classify(path: Path): SyncFile {
    val repo = findEnclosingRepo(path)
               ?: return SyncFile(path = path, kind = SyncFileKind.External, isWorkspaceFile = false)
    val name = path.name
    val kind = when {
      // directory, every rule target under it is in the scope.
      path.isDirectory() -> SyncFileKind.Folder(repo.folderLabelOf(path))

      // BUILD, BUILD.bazel. The directory of the file is the package.
      name in Constants.BUILD_FILE_NAMES -> SyncFileKind.Build(repo.labelOf(path.parent, AllRuleTargets))

      // MODULE.bazel, MODULE.bazel.lock, REPO.bazel
      name in MODULE_FILE_NAMES -> SyncFileKind.Module(repo.canonicalRepoName)

      // WORKSPACE, WORKSPACE.bazel, WORKSPACE.bzlmod
      name in WORKSPACE_FILE_NAMES -> SyncFileKind.Workspace

      // .bazelproject
      path.extension == Constants.PROJECT_VIEW_FILE_EXTENSION -> SyncFileKind.ProjectView

      // .bazelrc, .bazelignore, .bazelversion, .bazeliskrc, .bazeliskversion
      name in BAZEL_DOT_FILE_NAMES -> SyncFileKind.BazelDotFile

      // .bzl
      path.extension == "bzl" -> SyncFileKind.Starlark(label = repo.sourceLabelOf(path))

      // normal source file
      else -> repo.sourceLabelOf(path)?.let { label -> SyncFileKind.Source(label) } ?: SyncFileKind.External
    }
    return SyncFile(path = path, kind = kind)
  }

  private fun findEnclosingRepo(path: Path): EnclosingRepo? {
    // TODO: profile and optimize, for large number of local repositories can get slow
    val (canonicalRepoName, repoPath) = localRepositories.asSequence()
                                          .filter { (_, repoPath) -> path.startsWith(repoPath) }
                                          // find deepest innermost repository
                                          .maxByOrNull { (_, repoPath) -> repoPath.nameCount }
                                        ?: return null
    return EnclosingRepo(path = repoPath, canonicalRepoName = canonicalRepoName)
  }

  private class EnclosingRepo(val path: Path, val canonicalRepoName: String) {
    fun sourceLabelOf(file: Path): ResolvedLabel? {
      val packageDirectory = findPackageDirectory(file.parent) ?: return null
      val targetName = file.relativeToOrNull(packageDirectory) ?: return null
      return labelOf(packageDirectory, SingleTarget(targetName.invariantSeparatorsPathString))
    }

    fun labelOf(packageDirectory: Path, target: TargetType): ResolvedLabel =
      labelOf(Package(relativeSegmentsOf(packageDirectory)), target)

    fun folderLabelOf(directory: Path): ResolvedLabel =
      labelOf(AllPackagesBeneath(relativeSegmentsOf(directory)), AllRuleTargets)

    private fun labelOf(packagePath: PackageType, target: TargetType): ResolvedLabel =
      ResolvedLabel(
        repo = Canonical.createCanonicalOrMain(canonicalRepoName),
        packagePath = packagePath,
        target = target,
      )

    private fun relativeSegmentsOf(directory: Path): List<String> = directory.relativeToOrNull(path)?.segments().orEmpty()

    // TODO: cache that?
    private fun findPackageDirectory(directory: Path?): Path? =
      generateSequence(directory) { candidate -> candidate.parent }
        .takeWhile { candidate -> candidate.startsWith(path) }
        .firstOrNull { candidate -> Constants.BUILD_FILE_NAMES.any { buildFileName -> candidate.resolve(buildFileName).isRegularFile() } }

    private fun Path.segments(): List<String> = map { segment -> segment.toString() }.filter { segment -> segment.isNotEmpty() }
  }
}
