package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.ignore.BazelIgnoreService
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension

@ApiStatus.Internal
sealed class SimplifiedFileEvent private constructor(
  val fileRemoved: Path?,
  val fileAdded: Path?,
  val newVirtualFile: VirtualFile? = null,
) {
  private constructor(fileRemoved: String? = null, fileAdded: String? = null, newVirtualFile: VirtualFile? = null) : this(
    fileRemoved = fileRemoved?.toNioPathOrNull(),
    fileAdded = fileAdded?.toNioPathOrNull(),
    newVirtualFile = newVirtualFile,
  )

  /** The paths that the event affects. A move or a rename affects two. */
  fun affectedPaths(): List<Path> = listOfNotNull(fileRemoved, fileAdded)

  /** Returns true if Bazel reads the file itself, for example a BUILD or a `.bzl` file. */
  fun affectsBazelConfigFile(): Boolean =
    this !is CreateDirectory && affectedPaths().any { isBazelConfigFile(it) }

  /** Returns true if a Bazel target can own the file, so the workspace model may need an update. */
  fun affectsSourceFile(project: Project): Boolean =
    // the content of a source file never changes the target that owns it
    this !is ContentChange && affectedPaths().any { it.isSourceFile(project) }

  fun shouldBeProcessed(project: Project): Boolean {
    val rootPath = project.rootDir.toNioPath()
    if (affectedPaths().none { it.startsWith(rootPath) }) return false

    // Check for ignored files
    if (affectedPaths().all { BazelIgnoreService.getInstance(project).isIgnored(it) })
      return false

    // check for files under .bazelbsp
    val bazelBsp = rootPath.resolve(Constants.DOT_BAZELBSP_DIR_NAME)
    if (affectedPaths().all { it.startsWith(bazelBsp) })
      return false

    //
    return if (this is CreateDirectory) {
      fileAdded != null && newVirtualFile?.isValid == true
    }
    else {
      affectsBazelConfigFile() || affectsSourceFile(project)
    }
  }

  private fun Path.isSourceFile(project: Project): Boolean =
    LanguageClass.fromExtension(this.extension) != null &&
    !BazelIgnoreService.getInstance(project).isIgnored(this)

  fun doesAffectFolder(folderPath: Path): Boolean =
    fileRemoved?.startsWith(folderPath) == true || fileAdded?.startsWith(folderPath) == true

  @RequiresReadLock
  fun affectsExcludedFiles(fileIndex: ProjectFileIndex, fileSystem: LocalFileSystem): Boolean =
    newVirtualFile.isExcludedInFileIndex(fileIndex) ||
    fileRemoved?.getFirstExistingAncestor()?.let { fileSystem.findFileByNioFile(it) }.isExcludedInFileIndex(fileIndex)

  private fun VirtualFile?.isExcludedInFileIndex(fileIndex: ProjectFileIndex): Boolean =
    this?.let { fileIndex.isExcluded(it) } == true

  private fun Path.getFirstExistingAncestor(): Path? {
    var ancestor = parent
    while (ancestor != null && !ancestor.exists()) {
      ancestor = ancestor.parent
    }
    return ancestor
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SimplifiedFileEvent) return false

    if (fileRemoved != other.fileRemoved) return false
    if (fileAdded != other.fileAdded) return false
    if (newVirtualFile != other.newVirtualFile) return false

    return true
  }

  override fun hashCode(): Int {
    var result = fileRemoved.hashCode()
    result = 31 * result + fileAdded.hashCode()
    result = 31 * result + newVirtualFile.hashCode()
    return result
  }

  override fun toString(): String {
    return "${javaClass.simpleName}(removed=$fileRemoved, added=$fileAdded)"
  }

  class Create(path: String, vFile: VirtualFile?)
    : SimplifiedFileEvent(fileAdded = path, newVirtualFile = vFile) {
    constructor(originalEvent: VFileCreateEvent): this(originalEvent.path, originalEvent.file)
  }

  class CreateDirectory(originalEvent: VFileCreateEvent) :
    SimplifiedFileEvent(fileAdded = originalEvent.path, newVirtualFile = originalEvent.file)

  class Copy(originalEvent: VFileCopyEvent)
    : SimplifiedFileEvent(fileAdded = originalEvent.path, newVirtualFile = originalEvent.findCreatedFile())

  /**
   * A change of the file content, in practice a save.
   *
   * Bazel reads the new content of a configuration file, so the plugin must see the save. The path
   * goes into [fileAdded], because the file is present after the event. [affectsSourceFile] is
   * always false, so such an event never reaches the workspace model update.
   */
  class ContentChange(originalEvent: VFileContentChangeEvent)
    : SimplifiedFileEvent(fileAdded = originalEvent.path, newVirtualFile = originalEvent.file)

  class Delete(originalEvent: VFileDeleteEvent) : SimplifiedFileEvent(fileRemoved = originalEvent.path)

  class Move(originalEvent: VFileMoveEvent)
    : SimplifiedFileEvent(fileRemoved = originalEvent.oldPath, fileAdded = originalEvent.newPath, newVirtualFile = originalEvent.file)

  class Rename(originalEvent: VFilePropertyChangeEvent)
    : SimplifiedFileEvent(fileRemoved = originalEvent.oldPath, fileAdded = originalEvent.newPath, newVirtualFile = originalEvent.file)

  companion object {
    fun from(event: VFileEvent): SimplifiedFileEvent? =
      when (event) {
        is VFileCreateEvent ->
          if (event.isDirectory) {
            CreateDirectory(event)
          }
          else {
            Create(event)
          }
        is VFileCopyEvent -> Copy(event)
        is VFileContentChangeEvent -> ContentChange(event)
        is VFileDeleteEvent -> Delete(event)
        is VFileMoveEvent -> Move(event)
        is VFilePropertyChangeEvent -> {
          if (event.propertyName == VirtualFile.PROP_NAME) { // file rename
            Rename(event)
          } else { // property change other than file rename
            null
          }
        }

        else -> null
      }

    val SUPPORTED_CONFIG_FILE_NAMES: Set<String> = Constants.SUPPORTED_CONFIG_FILE_NAMES.toSet()

    /** Returns true if [file] names a Bazel configuration file. */
    fun isBazelConfigFile(file: Path): Boolean {
      return file.fileName.toString() in SUPPORTED_CONFIG_FILE_NAMES ||
             file.extension in Constants.SUPPORTED_EXTENSIONS
    }
  }
}
