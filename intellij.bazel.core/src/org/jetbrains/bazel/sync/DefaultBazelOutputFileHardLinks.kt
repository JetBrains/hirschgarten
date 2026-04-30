package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.openapi.vfs.newvfs.impl.NullVirtualFile
import com.intellij.util.io.createParentDirectories
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.relativeTo

internal class DefaultBazelOutputFileHardLinks(
  private val project: Project,
  bazelInfo: BazelInfo,
): BazelOutFileHardLinks {
  private val bazelOutputBase: Path = bazelInfo.outputBase

  /**
   * We can only create hard links in the same filesystem and same partition as Bazel's output base (BAZEL-3113).
   * To guarantee this we create hard links in Bazel's output base itself, see [Bazel docs](https://bazel.build/remote/output-directories#layout-diagram).
   * A side effect of this is that upon `bazel clean --expunge` in a workspace, the respective hard links will be deleted,
   * thereby preventing overly high disk usage. However, in that case the IDE will get red code.
   * Regular `bazel clean` or `--remote_download_minimal` won't have an effect as they only affect execroot, not the whole output base.
   */
  private val cacheDir: Path = bazelOutputBase.resolve("intellij-hardlinks")
  private val hardLinksDuringSync = ConcurrentHashMap<Path, Deferred<HardLink>>()
  private val syncRunning = AtomicBoolean(false)

  @Volatile
  override var allHardLinksCreatedSuccessfully: Boolean = true
    private set
  private class HardLink(val virtualFile: VirtualFile, val requiresRefresh: Boolean) {
    val isNull: Boolean
      get() = virtualFile == NullVirtualFile.INSTANCE

    companion object {
      val NULL = HardLink(NullVirtualFile.INSTANCE, false)
    }
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
  override suspend fun createOutputFileHardLink(originalFile: Path): Path? {
    if (!BazelFeatureFlags.hardLinkOutputFiles) return originalFile
    if (!syncRunning.get()) return originalFile

    if (!originalFile.exists()) return null
    val file = withContext(Dispatchers.IO) { originalFile.toRealPath() }
    if (file.startsWith(project.rootDir.toNioPath())) return originalFile  // It's a source file, no need to hard link

    // Hardlink only Bazel output files
    if (!file.startsWith(bazelOutputBase)) return originalFile
    val bazelOutRelativePath = file.relativeTo(bazelOutputBase)

    /**
     * Don't recreate the hard link unnecessarily to avoid spamming the file watcher (and because creating a link is expensive).
     * Also, the hard link may exist on disk, but if we delete the original file
     * and recreate it, then the link will point to the old version!
     */
    val targetHardLink = cacheDir.resolve(bazelOutRelativePath)
    val hardLink = hardLinksDuringSync.computeIfAbsent(targetHardLink) {
      BazelCoroutineService.getInstance(project).startAsync {
        withContext(Dispatchers.IO) {
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
          }
          catch (e: Throwable) {
            logger.warn("Failed to create hard link for $file", e)
            allHardLinksCreatedSuccessfully = false
            HardLink.NULL
          }
        }
      }
    }.await()  // Use await() on a Deferred instead of a blocking computeIfAbsent to avoid thread starvation (BAZEL-3095)

    return hardLink.takeIf { !it.isNull }?.virtualFile?.toNioPath() ?: file
  }

  override fun onBeforeSync() {
    syncRunning.set(true)
    allHardLinksCreatedSuccessfully = true
  }

  override suspend fun onAfterSync(projectModelUpdated: Boolean) {
    if (syncRunning.compareAndSet(true, false)) {
      if (!BazelFeatureFlags.hardLinkOutputFiles)
        return

      RefreshQueue.getInstance().refresh(
        recursive = false,
        hardLinksDuringSync.values.awaitAll().filter { it.requiresRefresh }.map { it.virtualFile },
      )

      if (projectModelUpdated) {
        // If sync failed and project model wasn't updated, the user will still see outputs from the previous sync and code won't be red.
        deleteUnusedHardLinks()
      }
      hardLinksDuringSync.clear()
    }
  }

  private suspend fun deleteUnusedHardLinks() {
    val cacheDirFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(cacheDir)
                       ?: return

    val hardLinksFilesUsedDuringSync = mutableSetOf(cacheDirFile)
    hardLinksDuringSync.values.awaitAll().filter { !it.isNull }.forEach { hardLink ->
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
      toDelete.forEach { it.delete(DefaultBazelOutputFileHardLinks) }
    }

    // Drop the old cache directories because the name is changed. Delete this code in 26.2
    NioFiles.deleteRecursively(project.getProjectDataPath("bazelOutputFilesHardLinks"))
    NioFiles.deleteRecursively(project.getProjectDataPath("bazel-out-hardlink"))
  }

  companion object {
    private val logger = logger<DefaultBazelOutputFileHardLinks>()
  }
}
