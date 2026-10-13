package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.intellij.openapi.diagnostic.rethrowControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.util.io.createParentDirectories
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.relativeTo

@ApiStatus.Internal
class DefaultBazelOutputFileHardLinks(
  private val project: Project,
  bazelInfo: BazelInfo,
): BazelOutFileHardLinks {
  private val bazelOutputBase: Path = bazelInfo.outputBase

  /**
   * We can only create hard links in the same filesystem and same partition as Bazel's output base (BAZEL-3113).
   * To guarantee this, we create hard links in Bazel's output base itself, see [Bazel docs](https://bazel.build/remote/output-directories#layout-diagram).
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

  /**
   * @param path the hard link, or the original file when [failed] is true
   */
  private class HardLink(val path: Path, val failed: Boolean = false)

  override suspend fun createOutputFileHardLinks(files: Collection<Path>): List<Path> {
    if (files.isEmpty()) return emptyList()
    if (!syncRunning.get()) return files.toList()

    var retainedPaths: MutableList<Path>? = null
    var hardLinkedPaths: MutableList<Deferred<HardLink>>? = null

    val realPaths: Map<Path, Deferred<Path?>> = coroutineScope {
      files.associateWith { originalFile ->
        async(limitedDispatcher) {
          originalFile.takeIf { it.exists() }?.toRealPath()
        }
      }
    }

    val rootDirPath = project.rootDir.toNioPath()
    for (originalFile in files) {
      val realFile = realPaths[originalFile]?.await() ?: continue

      if (realFile.startsWith(rootDirPath)) {
        // It's a source file, no need to hard link
        (retainedPaths ?: mutableListOf<Path>().also { retainedPaths = it }).add(originalFile)
        continue
      }

      // Hardlink only Bazel output files
      if (!realFile.startsWith(bazelOutputBase)) {
        (retainedPaths ?: mutableListOf<Path>().also { retainedPaths = it }).add(originalFile)
        continue
      }

      val bazelOutRelativePath = realFile.relativeTo(bazelOutputBase)

      /**
       * Don't recreate the hard link unnecessarily to avoid spamming the file watcher (and because creating a link is expensive).
       * Also, the hard link may exist on disk, but if we delete the original file
       * and recreate it, then the link will point to the old version!
       */
      val targetHardLink = cacheDir.resolve(bazelOutRelativePath)
      // Use await() on a Deferred instead of a blocking computeIfAbsent to avoid thread starvation (BAZEL-3095)
      val hardLink = hardLinksDuringSync.computeIfAbsent(targetHardLink) {
        BazelCoroutineService.getInstance(project).startAsync {
          withContext(limitedDispatcher) {
            try {
              if (!targetHardLink.exists() || targetHardLink.getLastModifiedTime() != realFile.getLastModifiedTime()) {
                targetHardLink.deleteIfExists()
                targetHardLink.createParentDirectories()
                Files.createLink(targetHardLink, realFile)
              }
              HardLink(targetHardLink)
            }
            catch (e: Throwable) {
              rethrowControlFlowException(e)
              logger.warn("Failed to create hard link for $realFile", e)
              allHardLinksCreatedSuccessfully = false
              HardLink(realFile, failed = true)
            }
          }
        }
      }
      (hardLinkedPaths ?: mutableListOf<Deferred<HardLink>>().also { hardLinkedPaths = it }).add(hardLink)
    }

    if (hardLinkedPaths == null)
      return retainedPaths ?: emptyList()

    return (retainedPaths ?: emptyList()) + hardLinkedPaths.awaitAll().map { it.path }
  }

  override fun onBeforeSync() {
    hardLinksDuringSync.clear()
    syncRunning.set(true)
    allHardLinksCreatedSuccessfully = true
  }

  override suspend fun onBeforeProjectModelUpdate() {
    if (!syncRunning.get()) return
    hardLinksDuringSync.values.awaitAll()
    refreshCacheDir()
  }

  override suspend fun onAfterSync(projectModelUpdated: Boolean) {
    if (syncRunning.compareAndSet(true, false)) {
      try {
        val hardLinks = hardLinksDuringSync.values.awaitAll()
        if (projectModelUpdated) {
          // If sync failed and project model wasn't updated, the user will still see outputs from the previous sync and code won't be red.
          deleteUnusedHardLinks(hardLinks)
        }
        refreshCacheDir()
      }
      finally {
        hardLinksDuringSync.clear()
      }
    }
  }

  /**
   * Updates the VFS with all changes in [cacheDir] in one refresh session.
   * The file watcher does not watch [cacheDir] (BAZEL-3612), so no file in it is dirty.
   * A recursive refresh skips a file that is not dirty, so [cacheDir] is marked dirty first.
   */
  private suspend fun refreshCacheDir() {
    val cacheDirFile = withContext(Dispatchers.IO) {
      LocalFileSystem.getInstance().refreshAndFindFileByNioFile(cacheDir)
    } ?: return
    VfsUtil.markDirty(/* recursive = */ true, /* reloadChildren = */ false, cacheDirFile)
    RefreshQueue.getInstance().refresh(recursive = true, listOf(cacheDirFile))
  }

  /**
   * Deletes each file and directory in [cacheDir] that the sync did not use.
   *
   * A used path is a hard link of this sync, or a directory that contains one.
   * The cleanup lists the children of the used directories on disk, not in the VFS (BAZEL-3494, BAZEL-3647).
   * It deletes an unused directory with all its contents.
   */
  private suspend fun deleteUnusedHardLinks(hardLinks: List<HardLink>) = withContext(Dispatchers.IO) {
    val pathsUsedDuringSync = hardLinks.asSequence()
      .filterNot { it.failed }
      .flatMap { hardLink -> generateSequence(hardLink.path) { it.parent }.takeWhile { it != cacheDir } }
      .plusElement(cacheDir)  // plus() would add the name elements of the path, because Path is Iterable
      .toHashSet()

    val usedDirectories = pathsUsedDuringSync.filter { it.isDirectory(LinkOption.NOFOLLOW_LINKS) }
    val unusedPaths = usedDirectories
      .flatMap { it.listDirectoryEntries() }
      .filterNot { it in pathsUsedDuringSync }
    unusedPaths.forEach(NioFiles::deleteRecursively)

    // Drop the old cache directories because the name is changed. Delete this code in 26.2
    NioFiles.deleteRecursively(project.getProjectDataPath("bazelOutputFilesHardLinks"))
    NioFiles.deleteRecursively(project.getProjectDataPath("bazel-out-hardlink"))
  }

  companion object {
    private val logger = logger<DefaultBazelOutputFileHardLinks>()
    private val limitedDispatcher = Dispatchers.IO.limitedParallelism(8)
  }
}
