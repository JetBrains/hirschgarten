package org.jetbrains.bsp.protocol.og


import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Path

/** Represents a workspace root  */
class WorkspaceRoot(private val directory: File) {
  fun fileForPath(workspacePath: WorkspacePath): File {
    return File(directory, workspacePath.relativePath())
  }

  fun directory(): File {
    return directory
  }

  fun absolutePathFor(workspaceRelativePath: String?): Path {
    return path().resolve(workspaceRelativePath)
  }

  fun path(): Path {
    return directory.toPath()
  }

  fun workspacePathFor(file: File): WorkspacePath {
    return workspacePathFor(file.path)
  }

  fun workspacePathFor(file: VirtualFile): WorkspacePath {
    return workspacePathFor(file.path)
  }

  fun relativize(file: VirtualFile): Path {
    return workspacePathFor(file).asPath()
  }

  fun tryRelativize(file: VirtualFile): Path? {
    if (!isInWorkspace(file)) {
      return null
    }

    return relativize(file)
  }

  private fun workspacePathFor(path: String): WorkspacePath {
    require(isInWorkspace(path)) { String.format("File '%s' is not under workspace %s", path, directory) }
    if (directory.path.length == path.length) {
      return WorkspacePath("")
    }
    return WorkspacePath(path.substring(directory.path.length + 1))
  }

  /**
   * Returns the WorkspacePath for the given absolute file, if it's a child of this WorkspaceRoot
   * and a valid WorkspacePath. Otherwise returns null.
   */
  fun workspacePathForSafe(absoluteFile: File): WorkspacePath? {
    return workspacePathForSafe(absoluteFile.path)
  }

  /**
   * Returns the WorkspacePath for the given virtual file, if it's a child of this WorkspaceRoot and
   * a valid WorkspacePath. Otherwise returns null.
   */
  fun workspacePathForSafe(file: VirtualFile): WorkspacePath? {
    return workspacePathForSafe(file.path)
  }

  private fun workspacePathForSafe(path: String): WorkspacePath? {
    if (!isInWorkspace(path)) {
      return null
    }
    if (directory.path.length == path.length) {
      return WorkspacePath("")
    }
    return WorkspacePath.createIfValid(path.substring(directory.path.length + 1))
  }

  fun isInWorkspace(file: File): Boolean {
    return isInWorkspace(file.path)
  }

  fun isInWorkspace(file: VirtualFile): Boolean {
    return isInWorkspace(file.path)
  }

  private fun isInWorkspace(path: String): Boolean {
    return FileUtil.isAncestor(directory.path, path, false)
  }

  override fun toString(): String {
    return directory.toString()
  }

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }
    if (o == null || javaClass != o.javaClass) {
      return false
    }

    val that = o as WorkspaceRoot
    return directory == that.directory
  }

  override fun hashCode(): Int {
    return directory.hashCode()
  }
}
