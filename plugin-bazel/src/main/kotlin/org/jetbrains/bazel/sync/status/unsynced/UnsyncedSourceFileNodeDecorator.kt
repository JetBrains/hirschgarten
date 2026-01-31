package org.jetbrains.bazel.sync.status.unsynced

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.utils.SourceType
import java.awt.Color

private val RELATED_UNSYNCED_SOURCE_TYPES = setOf(SourceType.GO)

internal val UNSYNCED_COLOR: JBColor = JBColor(Color(252, 234, 234), Color(94, 56, 56))

private class UnsyncedSourceFileNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (!project.isBazelProject) return
    if (project.isSyncInProgress()) return
    val vFile = node.virtualFile ?: return
    if (vFile.isUnsyncedSourceFile(project)) {
      data.addText(vFile.name + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
      data.addText(BazelPluginBundle.message("sync.status.unsynced.source.file.hint"), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
      data.background = UNSYNCED_COLOR
    }
  }
}

internal fun VirtualFile.isUnsyncedSourceFile(project: Project): Boolean {
  val extension = extension ?: return false
  if (SourceType.fromExtension(extension) !in RELATED_UNSYNCED_SOURCE_TYPES) return false
  if (!toNioPath().startsWith(project.rootDir.toNioPath())) return false
  val targetUtils = project.targetUtils
  return targetUtils.getTargetsForFile(this).isEmpty()
}
