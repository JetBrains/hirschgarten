package org.jetbrains.bazel.flow.vcs

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsRootError
import com.intellij.openapi.vcs.VcsRootErrorFilter
import com.intellij.openapi.vfs.toNioPathOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.flow.exclude.BazelSymlinkExcludeService
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.utils.isWindowsJunction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * [BAZEL-948](https://youtrack.jetbrains.com/issue/BAZEL-948): Bazel's symlink forest plants a `.git` symlink into the execution root,
 * so the execution root, and the Bazel convenience symlinks pointing into it, look like Git repositories.
 * Git running over such a root processes the whole Bazel output tree, which can freeze a large project.
 *
 * The invariant: the Bazel plugin keeps VCS roots out of the Bazel output directories, and only out of them.
 * A Bazel output directory is one under a Bazel convenience symlink of the workspace (`bazel-bin`, `bazel-out`, `bazel-<workspace>`, ...),
 * or one that really lies in the tree such a symlink or the execution root points to.
 * Every other VCS root is left to the platform VCS root detection, as if there was no Bazel.
 *
 * As a [VcsRootErrorFilter], it keeps the VCS root detection from reporting, and thus auto-registering, roots in the Bazel output directories.
 * Detection reaches them through a Bazel symlink that isn't excluded yet,
 * or by walking up from a content root in the execution root, e.g., a Python import root under `bazel-bin`.
 * Only the unregistered roots in the Bazel output directories are dropped; every other error is left to the platform.
 * The mappings persisted before are removed by [removeBazelOutputMappings].
 */
@ApiStatus.Internal
class BazelVcsRootErrorFilter : VcsRootErrorFilter {
  override fun filterErrors(project: Project, errors: Collection<VcsRootError>): Collection<VcsRootError> {
    if (!project.isBazelProject || errors.none { it.type == VcsRootError.Type.UNREGISTERED_ROOT }) return errors
    val isBazelOutputDirectory = bazelOutputDirectoryPredicate(project)
    return errors.filterNot { it.type == VcsRootError.Type.UNREGISTERED_ROOT && isBazelOutputDirectory(it.mapping.directory) }
  }

  interface IsBazelOutputDirectoryPredicate {
    operator fun invoke(directory: String): Boolean
  }

  companion object {
    /**
     * Returns whether a VCS root directory lies in a Bazel output directory.
     *
     * The real output trees come from the Bazel symlinks [BazelSymlinkExcludeService] has already collected and from the execution root.
     * A directory under a Bazel symlink is recognized even before that collection finishes, by checking its own path.
     */
    fun bazelOutputDirectoryPredicate(project: Project): IsBazelOutputDirectoryPredicate {
      val workspace = project.projectCtx.projectRootDir?.toNioPathOrNull()
      val knownSymlinks = BazelSymlinkExcludeService.getInstance(project).getBazelSymlinksToExclude()
      val outputTrees = (knownSymlinks + listOfNotNull(project.projectCtx.bazelExecPath)).mapNotNullTo(mutableSetOf()) { it.toRealPathOrNull() }

      return object : IsBazelOutputDirectoryPredicate {
        override fun invoke(directory: String): Boolean {
          val path = directory.toNioPathOrNull() ?: return false
          if (knownSymlinks.any { path.startsWith(it) }) return true
          // VCS root detection can run before BazelSymlinkExcludeStartupActivity finishes collecting the symlinks
          if (workspace != null && isUnderBazelSymlink(workspace, path)) return true
          if (outputTrees.isEmpty()) return false
          val realPath = path.toRealPathOrNull() ?: return false
          return outputTrees.any { realPath.startsWith(it) }
        }
      }
    }

    /**
     * Removes the VCS mappings in the Bazel output directories, e.g., the ones an earlier session persisted in `vcs.xml`.
     * Called at project initialization, before the VCS initialization activates Git for them;
     * afterwards, [BazelVcsRootErrorFilter] keeps new ones from being auto-registered.
     */
    suspend fun removeBazelOutputMappings(project: Project) {
      val manager = project.serviceAsync<ProjectLevelVcsManager>()
      val mappings = manager.getDirectoryMappings()
      if (mappings.isEmpty() || mappings.all { it.isDefaultMapping })
        return

      val (bazelOutputMappings, otherMappings) = withContext(Dispatchers.IO) {
        val isBazelOutputDirectory = bazelOutputDirectoryPredicate(project)
        mappings.partition { !it.isDefaultMapping && isBazelOutputDirectory(it.directory) }
      }
      if (bazelOutputMappings.isEmpty())
        return

      log.info("Removing VCS mappings in Bazel output directories: ${bazelOutputMappings.map { it.directory }}")
      manager.setDirectoryMappings(otherMappings)
    }

    /**
     * Whether [path] lies under a Bazel symlink of [workspace], looking as deep as [BazelSymlinksCalculator] scans for them.
     */
    private fun isUnderBazelSymlink(workspace: Path, path: Path): Boolean {
      if (!path.startsWith(workspace)) return false
      val relativePath = workspace.relativize(path)
      if (relativePath.toString().isEmpty()) return false
      return (1..minOf(relativePath.nameCount, BazelFeatureFlags.symlinkScanMaxDepth)).any { depth ->
        val ancestor = workspace.resolve(relativePath.subpath(0, depth))
        // Check the link first: isBazelSymlink logs every regular directory that ends like a Bazel symlink, e.g., `bin`
        (Files.isSymbolicLink(ancestor) || ancestor.isWindowsJunction) && BazelSymlinksCalculator.isBazelSymlink(workspace.name, ancestor)
      }
    }

    private fun Path.toRealPathOrNull(): Path? =
      try {
        toRealPath()
      }
      catch (_: IOException) {
        null
      }

    private val log = logger<BazelVcsRootErrorFilter>()
  }
}
