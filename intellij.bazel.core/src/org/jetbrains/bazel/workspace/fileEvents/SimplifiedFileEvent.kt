package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.ignore.BazelIgnoreService
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension

internal sealed class SimplifiedFileEvent private constructor(
  val fileRemoved: Path?,
  val fileAdded: Path?,
  val newVirtualFile: VirtualFile? = null,
) {
  private constructor(fileRemoved: String? = null, fileAdded: String? = null, newVirtualFile: VirtualFile? = null) : this(
    fileRemoved = fileRemoved?.toNioPathOrNull(),
    fileAdded = fileAdded?.toNioPathOrNull(),
    newVirtualFile = newVirtualFile,
  )

  // either of the affected files is a source file
  fun shouldBeProcessed(project: Project): Boolean =
    if (this is CreateDirectory) {
      val path = this.fileAdded
      return path != null && newVirtualFile?.isValid == true && !BazelIgnoreService.getInstance(project).isIgnored(path)
    } else {
      return fileRemoved?.shouldProcess(project) == true ||
             fileAdded?.shouldProcess(project) == true
    }

  private fun Path.shouldProcess(project: Project): Boolean =
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
  }

  class Create(path: String, vFile: VirtualFile?)
    : SimplifiedFileEvent(fileAdded = path, newVirtualFile = vFile) {
    constructor(originalEvent: VFileCreateEvent): this(originalEvent.path, originalEvent.file)
  }

  class CreateDirectory(originalEvent: VFileCreateEvent) :
    SimplifiedFileEvent(fileAdded = originalEvent.path, newVirtualFile = originalEvent.file) {
  }

  class Copy(originalEvent: VFileCopyEvent)
    : SimplifiedFileEvent(fileAdded = originalEvent.path, newVirtualFile = originalEvent.findCreatedFile())

  class Delete(originalEvent: VFileDeleteEvent) : SimplifiedFileEvent(fileRemoved = originalEvent.path)

  class Move(originalEvent: VFileMoveEvent)
    : SimplifiedFileEvent(fileRemoved = originalEvent.oldPath, fileAdded = originalEvent.newPath, newVirtualFile = originalEvent.file)

  class Rename(originalEvent: VFilePropertyChangeEvent)
    : SimplifiedFileEvent(fileRemoved = originalEvent.oldPath, fileAdded = originalEvent.newPath, newVirtualFile = originalEvent.file) {
  }
}
