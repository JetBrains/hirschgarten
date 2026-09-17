package org.jetbrains.bazel.sync.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.sync.ProjectSyncScope

internal fun VirtualFile.isResyncable(): Boolean =
  (isDirectory && Constants.BUILD_FILE_NAMES.any { this.findChild(it) != null })
  || this.name in Constants.BUILD_FILE_NAMES

internal sealed class ResyncFilesAction :
  BazelSyncAction({ BazelPluginBundle.message("resync.files.and.directories.action.text") }, BazelPluginIcons.bazelReload) {
  protected abstract fun selectedFiles(e: AnActionEvent): List<VirtualFile>

  private fun resyncableFiles(e: AnActionEvent): List<VirtualFile> = selectedFiles(e).filter { it.isResyncable() }

  override fun computeScope(project: Project, e: AnActionEvent): ProjectSyncScope? {
    val paths = resyncableFiles(e).mapNotNull { it.toNioPathOrNull() }.distinct()
    return if (paths.isEmpty()) {
      null
    }
    else {
      ProjectSyncScope.Files(files = paths, build = false)
    }
  }

  override fun shouldBeVisible(project: Project, e: AnActionEvent): Boolean = resyncableFiles(e).isNotEmpty()

  override fun updatePresentation(project: Project, e: AnActionEvent) {
    val files = resyncableFiles(e)
    val directories = files.count { it.isDirectory }
    e.presentation.text = when (directories) {
      files.size -> BazelPluginBundle.message("resync.directories.action.text", files.size)
      0 -> BazelPluginBundle.message("resync.files.action.text", files.size)
      else -> BazelPluginBundle.message("resync.files.and.directories.action.text")
    }
  }

  @Suppress("CanSealedSubClassBeObject")
  class XmlRegistered : ResyncFilesAction() {
    override fun selectedFiles(e: AnActionEvent): List<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.asList().orEmpty()
  }

  class NonXmlRegistered(private val files: List<VirtualFile>) : ResyncFilesAction() {
    override fun selectedFiles(e: AnActionEvent): List<VirtualFile> = files
  }
}
