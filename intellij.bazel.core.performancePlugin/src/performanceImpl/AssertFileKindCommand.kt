package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.application.readAction
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.rootDir

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
        val workspaceFileIndex = WorkspaceFileIndex.getInstance(project)
        file != null && fileKindCheck.verify(workspaceFileIndex, file)
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
  INDEXABLE {
    override fun verify(workspaceFileIndex: WorkspaceFileIndex, file: VirtualFile): Boolean =
      workspaceFileIndex.isIndexable(file)

    override fun displayName(): String = "indexable"
  },
  NON_INDEXABLE {
    override fun verify(workspaceFileIndex: WorkspaceFileIndex, file: VirtualFile): Boolean =
      !workspaceFileIndex.isIndexable(file)

    override fun displayName(): String = "non-indexable"
  },
  IN_CONTENT {
    override fun verify(workspaceFileIndex: WorkspaceFileIndex, file: VirtualFile): Boolean =
      workspaceFileIndex.isInContent(file)

    override fun displayName(): String = "in project content"
  },
  OUTSIDE_CONTENT {
    override fun verify(workspaceFileIndex: WorkspaceFileIndex, file: VirtualFile): Boolean =
      !workspaceFileIndex.isInContent(file)

    override fun displayName(): String = "outside project content"
  };

  abstract fun verify(workspaceFileIndex: WorkspaceFileIndex, file: VirtualFile): Boolean

  abstract fun displayName(): String
}
