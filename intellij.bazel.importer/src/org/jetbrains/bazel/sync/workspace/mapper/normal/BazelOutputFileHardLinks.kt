package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.newvfs.impl.NullVirtualFile
import com.intellij.util.io.createParentDirectories
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.relativeTo

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class BazelOutputFileHardLinks(private val project: Project) {
  private val cacheDir: Path = project.getProjectDataPath("bazel-out-hardlink")
  private val hardLinksDuringSync = ConcurrentHashMap<Path, HardLink>()

  // https://bazel.build/remote/output-directories#layout
  private var bazelOutputPath: Path? = null

  // TODO: Refactor this class to be part of BazelServerFacade
  fun setBazelOutputPath(path: Path) {
    bazelOutputPath = path
  }

  private class HardLink(val virtualFile: VirtualFile, val requiresRefresh: Boolean) {
    val isNull: Boolean
      get() = virtualFile == NullVirtualFile.INSTANCE
  }

  /**
   * Creates a hard link to Bazel's outputs during sync (e.g., jars).
   *
   * The reason is that Bazel with `--remote_download_minimal` can garbage collect jars it thinks are "not needed"
   * from the output base (see https://youtrack.jetbrains.com/issue/BAZEL-3049), so we cannot rely on them always being there.
   * Using hard links also makes sure fsnotifier isn't spammed by file events whenever Bazel builds a target.
   *
   * NOTE: The operation is expensive, so consider async execution
   *
   * @return the created hard link, [originalFile] itself if it isn't an output file (e.g. a for source file), or `null` if [originalFile] doesn't exist
   */
  fun createOutputFileHardLink(originalFile: Path): Path? {
    if (!BazelFeatureFlags.hardLinkOutputFiles) return originalFile

    if (!originalFile.exists()) return null
    val file = originalFile.toRealPath()
    if (file.startsWith(project.rootDir.toNioPath())) return file  // It's a source file, no need to hard link

    // Hardlink files only under `bazel-out` directory
    if (bazelOutputPath == null || !file.startsWith(bazelOutputPath))
      return file
    val bazelOutRelativePath = file.relativeTo(bazelOutputPath!!)

    /**
     * Don't recreate the hard link unnecessarily to avoid spamming the file watcher (and because creating a link is expensive).
     * Also, the hard link may exist on disk, but if we delete the original file
     * and recreate it, then the link will point to the old version!
     */
    val targetHardLink = cacheDir.resolve(bazelOutRelativePath)
    val hardLink = hardLinksDuringSync.computeIfAbsent(targetHardLink) {
      try {
        val localFileSystem = LocalFileSystem.getInstance()
        var requiresRefresh = true
        val hardLinkFile = if (!targetHardLink.exists() || targetHardLink.getLastModifiedTime() != file.getLastModifiedTime()) {
          targetHardLink.deleteIfExists()
          targetHardLink.createParentDirectories()
          Files.createLink(targetHardLink, file)
          localFileSystem.refreshAndFindFileByNioFile(targetHardLink)
        }
        else {
          requiresRefresh = false
          localFileSystem.findFileByNioFile(targetHardLink) ?: localFileSystem.refreshAndFindFileByNioFile(targetHardLink)
        }
        checkNotNull(hardLinkFile) { "Can't find virtual find for $targetHardLink" }
        HardLink(hardLinkFile, requiresRefresh)
      } catch (e: Throwable) {
        logger.warn("Failed to create hard link for $file", e)
        HardLink(NullVirtualFile.INSTANCE, false)
      }
    }

    return hardLink.takeIf { !it.isNull }?.virtualFile?.toNioPath() ?: file
  }

  fun createOutputFileHardLinkAsync(originalFile: Path): Deferred<Path?> {
    return BazelCoroutineService.getInstance(project).startAsync {
      withContext(Dispatchers.IO) {
        createOutputFileHardLink(originalFile)
      }
    }
  }

  suspend fun createOutputFileHardLinks(files: Collection<Path>): List<Path> =
    files.map { createOutputFileHardLinkAsync(it) }.awaitAll().filterNotNull()

  private suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    hardLinksDuringSync.values.forEach { hardLink ->
      if (!hardLink.isNull && hardLink.requiresRefresh)
        hardLink.virtualFile.refresh(false, false)
    }

    if (environment.projectModelUpdated) {
      // If sync failed and project model wasn't updated, the user will still see outputs from the previous sync and code won't be red.
      deleteUnusedHardLinks()
    }
    hardLinksDuringSync.clear()
  }

  private suspend fun deleteUnusedHardLinks() {
    val cacheDirFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(cacheDir)
                       ?: return

    val hardLinksFilesUsedDuringSync = mutableSetOf(cacheDirFile)
    hardLinksDuringSync.values.filter { !it.isNull }.forEach { hardLink ->
      var parent: VirtualFile? = hardLink.virtualFile
      while (parent != null && hardLinksFilesUsedDuringSync.add(parent)) {
        parent = parent.parent
      }
    }

    val toDelete = mutableListOf<VirtualFile>()
    VfsUtilCore.visitChildrenRecursively(
      cacheDirFile,
      object : VirtualFileVisitor<Nothing>() {
        override fun visitFile(file: VirtualFile): Boolean {
          if (file !in hardLinksFilesUsedDuringSync) {
            toDelete.add(file)
            return false
          }
          return true
        }
      },
    )

    writeAction {
      toDelete.forEach { it.delete(BazelOutputFileHardLinks) }
    }

    // Drop the old directory because the name is changed. Delete this code in 26.2
    NioFiles.deleteRecursively(project.getProjectDataPath("bazelOutputFilesHardLinks"))
  }

  internal class PostSyncHook : ProjectPostSyncHook {
    override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
      if (!BazelFeatureFlags.hardLinkOutputFiles) return
      getInstance(environment.project).onPostSync(environment)
    }
  }

  companion object {
    private val logger = logger<BazelOutputFileHardLinks>()

    @JvmStatic
    fun getInstance(project: Project): BazelOutputFileHardLinks = project.service()
  }
}

@ApiStatus.Internal
fun refreshVfsAfterBazelBuild() {
  if (BazelFeatureFlags.hardLinkOutputFiles) {
    // asyncRefresh() refreshes the whole VFS, which includes all projects that have ever been opened in the IDE, not just the current one.
    // That can lead to long UI freezes, which is why asyncRefresh() is now deprecated in IJ platform.
    // On the other hand, BazelOutputFileHardLinks handles VFS refresh granularly:
    // only on project sync, and only for changed Bazel outputs that are used in the current project.
    return
  }
  VirtualFileManager.getInstance().asyncRefresh()
}
