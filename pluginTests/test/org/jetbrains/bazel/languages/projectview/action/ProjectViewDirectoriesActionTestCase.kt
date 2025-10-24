package org.jetbrains.bazel.languages.projectview.action

import com.intellij.notification.Notification
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.io.createDirectory
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.settings.bazel.BazelProjectSettings
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.test.framework.BazelPathManager
import java.nio.file.Path
import kotlin.io.path.Path

abstract class ProjectViewDirectoriesActionTestCase(
  protected val actionId: String,
) : BasePlatformTestCase() {

  override fun getProjectDescriptor() = LightProjectDescriptor()

  protected val bazelProjectView
    get() = runWithModalProgressBlocking(project, "Getting project view") {
      project.service<ProjectViewService>().getProjectView()
    }

  protected val action: AnAction get() = ActionManager.getInstance().getAction(actionId)

  protected val notifications: List<Notification>
    get() = NotificationsManager
      .getNotificationsManager()
      .getNotificationsOfType(Notification::class.java, project)
      .asList()

  protected fun testPresentationOn(context: DataContext): Presentation {
    val event = TestActionEvent.createTestEvent(context)
    ActionUtil.updateAction(action, event)
    return event.presentation
  }

  protected fun openProjectViewInEditor(path: String) {
    val projectViewFile = BazelPathManager
      .getTestFixturePath(path)
      .toVirtualFile()
    project.bazelProjectSettings = BazelProjectSettings(projectViewPath = projectViewFile.toNioPath())
    myFixture.openFileInEditor(projectViewFile)
  }

  protected fun performActionOnProjectDir(directory: String, actionId: String = this.actionId) {
    val dir = Path(checkNotNull(myFixture.project.basePath))
      .createDirectory(directory)
      .toVirtualFile()
    val context = createActionContext(dir)
    myFixture.performEditorAction(actionId, TestActionEvent.createTestEvent(context))
  }

  protected fun createActionContext(file: VirtualFile? = null) = SimpleDataContext.builder()
    .add(CommonDataKeys.PROJECT, project)
    .let { if (file != null) it.add(CommonDataKeys.VIRTUAL_FILE, file) else it }
    .build()

  private fun Path.toVirtualFile() = project
    .service<WorkspaceModel>()
    .getVirtualFileUrlManager()
    .let(this::toVirtualFileUrl)
    .virtualFile
    .let(::checkNotNull)
}
