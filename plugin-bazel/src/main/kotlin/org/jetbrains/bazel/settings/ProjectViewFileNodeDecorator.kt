package org.jetbrains.bazel.settings

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject

class ProjectViewFileNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>?, data: PresentationData?) {
    if (data == null || node == null || !project.isBazelProject) return
    val vFile = node.virtualFile ?: return
    if (!vFile.isProjectViewFile()) return
    if (project.bazelProjectSettings.projectViewPath != vFile.toNioPath().toAbsolutePath()) return
    data.addText(vFile.name + " ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
    data.addText(BazelPluginBundle.message("project.view.tree.project.view.file.hint"), SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
  }

  private fun VirtualFile.isProjectViewFile(): Boolean = extension == BazelPluginConstants.PROJECT_VIEW_FILE_EXTENSION
}
