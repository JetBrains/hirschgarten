package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.openapi.vfs.newvfs.impl.NullVirtualFile
import com.intellij.util.io.createParentDirectories
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import java.io.IOException
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.createLinkPointingTo
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.readAttributes
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
  @VisibleForTesting
  val cacheDir: Path = bazelOutputBase.resolve("intellij-hardlinks")
  private val hardLinksDuringSync = ConcurrentHashMap<Path, Deferred<HardLink>>()
  private val syncRunning = AtomicBoolean(false)

  @Volatile
  override var allHardLinksCreatedSuccessfully: Boolean = true
    private set

  private class HardLink(val virtualFile: VirtualFile, val requiresRefresh: Boolean, val originalPathIfFailed: Path? = null) {
    val path: Path
      get() = if (virtualFile == NullVirtualFile.INSTANCE) checkNotNull(originalPathIfFailed) else virtualFile.toNioPath()
  }

  override suspend fun createOutputFileHardLinks(files: Collection<Path>): List<Path> {
    if (files.isEmpty()) return emptyList()
    if (!syncRunning.get()) return files.toList()

    var retainedPaths: MutableList<Path>? = null
    var hardLinkedPaths: MutableList<Deferred<HardLink>>? = null

    val realPaths: Map<Path, Deferred<Path?>> = coroutineScope {
      files.associateWith { originalFile ->
        async(limitedDispatcher) {
          originalFile.takeIf { it.exists() && !it.isDirectory() }?.toRealPath()
        }
      }
    }

    val rootDirPath = project.rootDir.toNioPath()
    for (originalFile in files) {
      val realFile = realPaths[originalFile]?.await() ?: continue

      // Hardlink only Bazel output files
      if (!originalFile.startsWith(bazelOutputBase)) {
        (retainedPaths ?: mutableListOf<Path>().also { retainedPaths = it }).add(originalFile)
        continue
      }

      val bazelOutRelativePath = originalFile.relativeTo(bazelOutputBase)

      /**
       * Don't recreate the hard link unnecessarily to avoid spamming the file watcher (and because creating a link is expensive).
       * Also, the hard link may exist on disk, but if we delete the original file
       * and recreate it, then the link will point to the old version!
       */
      val targetHardLink = cacheDir.resolve(bazelOutRelativePath)
      // Use await() on a Deferred instead of a blocking computeIfAbsent to avoid thread starvation (BAZEL-3095)
      val hardLink = hardLinksDuringSync.computeIfAbsent(targetHardLink) { targetHardLink ->
        BazelCoroutineService.getInstance(project).startAsync {
          withContext(limitedDispatcher) {
            try {
              val fileManager = VirtualFileManager.getInstance()
              val targetHardLinkAttributes =
                runCatching { targetHardLink.readAttributes<BasicFileAttributes>(LinkOption.NOFOLLOW_LINKS) }.getOrNull()
              val isUpToDate = when {
                targetHardLinkAttributes == null -> false
                // Symbolic link. If the target stays the same, we don't really know if the target file was modified,
                // hence the targetHardLinkAttributes?.isSymbolicLink check below in requiresRefresh
                shouldCreateSymLink(realFile, rootDirPath) ->
                  targetHardLinkAttributes.isSymbolicLink && runCatching { targetHardLink.toRealPath() }.getOrNull() == realFile
                // Hard link. Bazel always deletes and recreates a file when modifying it,
                // meaning the hard link is gonna point to a deleted file with an older timestamp in that case.
                else -> !targetHardLinkAttributes.isSymbolicLink && targetHardLinkAttributes.lastModifiedTime() == realFile.getLastModifiedTime()
              }
              val hardLinkFile = if (!isUpToDate) {
                targetHardLink.deleteIfExists()
                targetHardLink.createParentDirectories()
                createHardLinkOrSymbolicLink(targetHardLink, realFile, rootDirPath)
                fileManager.refreshAndFindFileByNioPath(targetHardLink)
              }
              else {
                fileManager.findFileByNioPath(targetHardLink) ?: fileManager.refreshAndFindFileByNioPath(targetHardLink)
              }
              checkNotNull(hardLinkFile) { "Can't find virtual find for $targetHardLink" }
              HardLink(hardLinkFile, requiresRefresh = !isUpToDate || targetHardLinkAttributes?.isSymbolicLink == true)
            }
            catch (e: Throwable) {
              logger.warn("Failed to create hard link for $realFile", e)
              allHardLinksCreatedSuccessfully = false
              HardLink(NullVirtualFile.INSTANCE, false, originalPathIfFailed = realFile)
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

  private fun createHardLinkOrSymbolicLink(targetHardLink: Path, realFile: Path, rootDirPath: Path) {
    if (shouldCreateSymLink(realFile, rootDirPath)) {
      // realFile is a source file, meaning the originalFile was a symlink into the source tree.
      // Let's try to create a symlink instead, so that we know where to find the original file (used, e.g., by the CLion engine)
      try {
        targetHardLink.createSymbolicLinkPointingTo(realFile)
        return
      }
      catch (e: IOException) {
        logger.debug("Failed to create symlink, Windows without Developer Mode?", e)
      }
    }
    targetHardLink.createLinkPointingTo(realFile)
  }

  private fun shouldCreateSymLink(realFile: Path, rootDirPath: Path): Boolean {
    // If realFile is a source file, that means the originalFile was a symlink into the source tree.
    // In that case we should try to create a symlink instead, so that we know where to find the original file (used by CLion: CPP-52156)
    return realFile.startsWith(rootDirPath)
  }

  override fun onBeforeSync() {
    syncRunning.set(true)
    allHardLinksCreatedSuccessfully = true
  }

  override suspend fun onAfterSync(fullProjectModelUpdated: Boolean) {
    if (syncRunning.compareAndSet(true, false)) {
      RefreshQueue.getInstance().refresh(
        recursive = false,
        hardLinksDuringSync.values.awaitAll().filter { it.requiresRefresh }.map { it.virtualFile },
      )

      if (fullProjectModelUpdated) {
        // If sync failed and project model wasn't updated, the user will still see outputs from the previous sync and code won't be red.
        deleteUnusedHardLinks()
      }
      hardLinksDuringSync.clear()
    }
  }

  override fun resolveCachedPath(fileOrDir: Path): Path {
    if (!fileOrDir.startsWith(bazelOutputBase)) return fileOrDir
    val bazelOutRelativePath = fileOrDir.relativeTo(bazelOutputBase)
    return cacheDir.resolve(bazelOutRelativePath)
  }

  private suspend fun deleteUnusedHardLinks() {
    val cacheDirFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(cacheDir)
                       ?: return

    val hardLinksFilesUsedDuringSync = mutableSetOf(cacheDirFile)
    hardLinksDuringSync.values.awaitAll().filter { it.virtualFile != NullVirtualFile.INSTANCE }.forEach { hardLink ->
      var parent: VirtualFile? = hardLink.virtualFile
      while (parent != null && hardLinksFilesUsedDuringSync.add(parent)) {
        parent = parent.parent
      }
    }

    val toDeleteVF = mutableListOf<VirtualFile>()
    VfsUtilCore.visitChildrenRecursively(
      cacheDirFile,
      object : VirtualFileVisitor<Nothing>() {
        override fun visitFile(file: VirtualFile): Boolean {
          if (file !in hardLinksFilesUsedDuringSync) {
            toDeleteVF.add(file)
            return false
          }
          return true
        }
      },
    )

    // https://youtrack.jetbrains.com/issue/BAZEL-3494
    val toDeleteNio = readAction {
      toDeleteVF.asSequence().filter { it.isValid }.map { it.toNioPath() }.toList()
    }
    for (path in toDeleteNio) {
      if (path.isDirectory()) {
        NioFiles.deleteRecursively(path)
      } else {
        path.deleteIfExists()
      }
    }
    RefreshQueue.getInstance().refresh(true, toDeleteVF)

    // Drop the old cache directories because the name is changed. Delete this code in 26.2
    NioFiles.deleteRecursively(project.getProjectDataPath("bazelOutputFilesHardLinks"))
    NioFiles.deleteRecursively(project.getProjectDataPath("bazel-out-hardlink"))
  }

  companion object {
    private val logger = logger<DefaultBazelOutputFileHardLinks>()
    private val limitedDispatcher = Dispatchers.IO.limitedParallelism(8)
  }
}
