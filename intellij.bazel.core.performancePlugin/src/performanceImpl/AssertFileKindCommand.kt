package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import com.intellij.workspaceModel.ide.legacyBridge.findModuleEntity
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.target.targetUtils

internal class AssertFileKindCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "assertFileKind"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val args = extractCommandArgument(PREFIX).trim().split(" ")
    val relativePath = args[0]
    val expectedFileKinds = args[1].split(",").map { FileKindCheck.valueOf(it) }

    val project = context.project
    val rootDir = project.rootDir
    val file = rootDir.findFileByRelativePath(relativePath)
    for (fileKindCheck in expectedFileKinds) {
      val actualState = readAction {
        file != null && fileKindCheck.verify(project, file)
      }

      check(actualState) {
        val fileKindCheckDisplayName = fileKindCheck.displayName()
        "Expected '$relativePath' to be $fileKindCheckDisplayName, but it was not"
      }
    }
  }
}

@ApiStatus.Internal
enum class FileKindCheck {
  IN_TARGETS {
    override fun verify(project: Project, file: VirtualFile): Boolean =
      project.targetUtils.getTargetsForFile(file).isNotEmpty()

    override fun displayName(): String = "in targets"
  },
  NOT_IN_TARGETS {
    override fun verify(project: Project, file: VirtualFile): Boolean =
      project.targetUtils.getTargetsForFile(file).isEmpty()

    override fun displayName(): String = "not in targets"
  },
  IN_WSM {
    override fun verify(project: Project, file: VirtualFile): Boolean =
      ProjectRootManager.getInstance(project).fileIndex.getModulesForFile(file, true)
        .mapNotNull { it.findModuleEntity() }.isNotEmpty()

    override fun displayName(): String = "in workspace model"
  },
  NOT_IN_WSM {
    override fun verify(project: Project, file: VirtualFile): Boolean =
      ProjectRootManager.getInstance(project).fileIndex.getModulesForFile(file, true)
        .mapNotNull { it.findModuleEntity() }.isEmpty()

    override fun displayName(): String = "outside workspace model"
  },
  INDEXABLE {
    override fun verify(project: Project, file: VirtualFile): Boolean =
      WorkspaceFileIndex.getInstance(project).isIndexable(file)

    override fun displayName(): String = "indexable"
  },
  NON_INDEXABLE {
    override fun verify(project: Project, file: VirtualFile): Boolean =
      !WorkspaceFileIndex.getInstance(project).isIndexable(file)

    override fun displayName(): String = "non-indexable"
  },
  IN_CONTENT {
    override fun verify(project: Project, file: VirtualFile): Boolean =
      WorkspaceFileIndex.getInstance(project).isInContent(file)

    override fun displayName(): String = "in project content"
  },
  OUTSIDE_CONTENT {
    override fun verify(project: Project, file: VirtualFile): Boolean =
      !WorkspaceFileIndex.getInstance(project).isInContent(file)

    override fun displayName(): String = "outside project content"
  };

  abstract fun verify(project: Project, file: VirtualFile): Boolean

  abstract fun displayName(): String
}
