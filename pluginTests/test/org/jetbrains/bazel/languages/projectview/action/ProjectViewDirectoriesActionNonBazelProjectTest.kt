package org.jetbrains.bazel.languages.projectview.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.workspaceModel.updateProjectModel
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntityFixtures.emptyBazelDirectoryWorkspaceEntity
import org.jetbrains.bazel.workspacemodel.entities.NonIndexableVirtualFileUrl
import org.junit.jupiter.api.Test

@TestApplication
class ProjectViewDirectoriesActionNonBazelProjectTest {

  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  private val projectFixture = projectFixture(pathFixture = tempDirFixture, openAfterCreation = true)
  private val project by projectFixture

  @Test
  fun `add-to-directories action is hidden and does not throw in a non-Bazel project`() {
    val directory = createDirectory("some-dir")
    assertActionHidden("Bazel.AddToProjectViewDirectoriesAction", directory)
  }

  @Test
  fun `exclude-from-directories action is hidden and does not throw in a non-Bazel project`() {
    val directory = createDirectory("some-dir")
    registerIncludedRoot(directory)
    assertActionHidden("Bazel.ExcludeFromProjectViewDirectoriesAction", directory)
  }

  private fun assertActionHidden(actionId: String, directory: VirtualFile) {
    IndexingTestUtil.waitUntilIndexesAreReady(project)
    val action = ActionManager.getInstance().getAction(actionId)
    val context = SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, project)
      .add(CommonDataKeys.VIRTUAL_FILE, directory)
      .build()
    val event = TestActionEvent.createTestEvent(context)
    ActionUtil.updateAction(action, event)
    event.presentation.isEnabledAndVisible shouldBe false
  }

  private fun createDirectory(path: String): VirtualFile {
    val root = tempDir.refreshAndFindVirtualDirectory() ?: error("Cannot find virtual directory for $path")
    return runWriteAction { root.findOrCreateDirectory(path) }
  }

  private fun registerIncludedRoot(directory: VirtualFile) {
    val workspaceModel = project.workspaceModel
    val urlManager = workspaceModel.getVirtualFileUrlManager()
    val entity = emptyBazelDirectoryWorkspaceEntity(project)
    entity.includedRoots = mutableListOf(NonIndexableVirtualFileUrl(directory.toVirtualFileUrl(urlManager)))
    runWriteAction {
      workspaceModel.updateProjectModel { storage -> storage.addEntity(entity) }
    }
  }
}
