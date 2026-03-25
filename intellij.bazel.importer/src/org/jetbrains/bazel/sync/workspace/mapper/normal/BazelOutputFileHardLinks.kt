package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.util.io.DigestUtil
import com.intellij.util.io.createParentDirectories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

@Service(Service.Level.PROJECT)
internal class BazelOutputFileHardLinks(private val project: Project) {
  private val cacheDir: Path = project.getProjectDataPath("bazelOutputFilesHardLinks")
  private val hardLinksUsedDuringSync: MutableSet<Path> = ConcurrentHashMap.newKeySet()
  private val hardLinksFilesUsedDuringSync: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet()
  private val hardLinkCreateJobs: MutableSet<Job> = ConcurrentHashMap.newKeySet()

  /**
   * Creates a hard link to Bazel's outputs during sync (e.g., jars).
   *
   * The reason is that Bazel with `--remote_download_minimal` can garbage collect jars it thinks are "not needed"
   * from the output base (see https://youtrack.jetbrains.com/issue/BAZEL-3049), so we cannot rely on them always being there.
   * Using hard links also makes sure fsnotifier isn't spammed by file events whenever Bazel builds a target.
   *
   * @return the created hard link, [originalFile] itself if it isn't an output file (e.g. a for source file), or `null` if [originalFile] doesn't exist
   */
  fun createOutputFileHardLink(originalFile: Path): Path? {
    if (!BazelFeatureFlags.hardLinkOutputFiles) return originalFile

    if (!originalFile.exists()) return null
    val file = originalFile.toRealPath()
    if (file.startsWith(project.rootDir.toNioPath())) return file  // It's a source file, no need to hard link

    // Compact the path to a sha256 hash to avoid MAX_PATH limitations on Windows, while keeping the original filename.
    val parentPathHash = DigestUtil.sha256Hex(file.parent.toString().toByteArray())
    val targetHardLink = cacheDir.resolve(parentPathHash).resolve(file.name)

    if (!hardLinksUsedDuringSync.add(targetHardLink)) {
      // Hard link already processed
      return targetHardLink
    }

    // Async, because Files.createLink is quite expensive (but parallelizable well as profiling shows)
    hardLinkCreateJobs += BazelCoroutineService.getInstance(project).start {
      withContext(Dispatchers.IO) {
        /**
         * Don't recreate the hard link unnecessarily to avoid spamming the file watcher (and because creating a link is expensive).
         * Also, the hard link may exist on disk, but if we delete the original file and recreate it, then the link will point to the old version!
         */
        val localFileSystem = LocalFileSystem.getInstance()
        val hardLinkFile = if (!targetHardLink.exists() || targetHardLink.getLastModifiedTime() != file.getLastModifiedTime()) {
          targetHardLink.deleteIfExists()
          targetHardLink.createParentDirectories()
          Files.createLink(targetHardLink, file)
          localFileSystem.refreshAndFindFileByNioFile(targetHardLink)?.also { hardLinkFile ->
            hardLinkFile.refresh(false, false)
          }
        }
        else {
          localFileSystem.findFileByNioFile(targetHardLink) ?: localFileSystem.refreshAndFindFileByNioFile(targetHardLink)
        }
        checkNotNull(hardLinkFile) { "Can't find virtual find for $targetHardLink" }
        hardLinksFilesUsedDuringSync += hardLinkFile
        hardLinksFilesUsedDuringSync += checkNotNull(hardLinkFile.parent)
      }
    }

    return targetHardLink
  }

  fun createOutputFileHardLinks(files: Collection<Path>): List<Path> =
    files.mapNotNull { createOutputFileHardLink(it) }

  private suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    hardLinkCreateJobs.joinAll()
    hardLinkCreateJobs.clear()
    hardLinksUsedDuringSync.clear()
    if (environment.projectModelUpdated) {
      // If sync failed and project model wasn't updated, the user will still see outputs from the previous sync and code won't be red.
      deleteUnusedHardLinks()
    }
    hardLinksFilesUsedDuringSync.clear()
  }

  @OptIn(ExperimentalPathApi::class)
  private suspend fun deleteUnusedHardLinks() {
    val cacheDirFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(cacheDir) ?: return
    hardLinksFilesUsedDuringSync.add(cacheDirFile)

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
  }

  internal class PostSyncHook : ProjectPostSyncHook {
    override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
      if (!BazelFeatureFlags.hardLinkOutputFiles) return
      getInstance(environment.project).onPostSync(environment)
    }
  }

  companion object {
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
