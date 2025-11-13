package org.jetbrains.bazel.languages.projectview.action

import android.databinding.tool.ext.mapEach
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.workspaceModel.updateProjectModel
import org.intellij.lang.annotations.Language
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.directories
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import kotlin.io.path.pathString

abstract class ProjectViewDirectoriesActionTestCase(
  protected val actionId: String,
) : BasePlatformTestCase() {

  protected val bazelProjectView
    get() = runWithModalProgressBlocking(project, "Getting project view") {
      project.service<ProjectViewService>().getProjectView()
    }

  protected val action: AnAction get() = ActionManager.getInstance().getAction(actionId)

  override fun setUp() {
    super.setUp()
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")
  }

  override fun getProjectDescriptor() = LightProjectDescriptor()

  protected fun testPresentationOn(context: DataContext): Presentation {
    val event = TestActionEvent.createTestEvent(context)
    ActionUtil.updateAction(action, event)
    return event.presentation
  }

  protected fun useProjectView(@Language("projectview") content: String) {
    val projectView = myFixture.tempDirFixture.createFile(".user.bazelproject", content)
    project.bazelProjectSettings = project.bazelProjectSettings.withNewProjectViewPath(projectView)
    // initializes editor with some file
    myFixture.openFileInEditor(myFixture.createFile("Foo.kt", ""))
    val workspaceModel = WorkspaceModel.getInstance(project)
    val manager = workspaceModel.getVirtualFileUrlManager()
    val root = myFixture.tempDirFixture.findOrCreateDir(".")
    val bazelProjectView = bazelProjectView
    runWithModalProgressBlocking(project, "Syncing project view...") {
      val (includes, excludes) = writeAction {
        bazelProjectView.directories
          .partition { it.isIncluded() }
          .mapEach { part -> part.map { root.findOrCreateDirectory(it.value.pathString) } }
      }
      val entity = BazelProjectDirectoriesEntity(
        projectRoot = root.toVirtualFileUrl(manager),
        includedRoots = includes.map { it.toVirtualFileUrl(manager) },
        excludedRoots = excludes.map { it.toVirtualFileUrl(manager) },
        indexAllFilesInIncludedRoots = false,
        indexAdditionalFiles = emptyList(),
        entitySource = BazelDummyEntitySource,
      )
      writeAction { workspaceModel.updateProjectModel { updater -> updater.addEntity(entity) } }
    }
  }

  protected fun performActionOnProjectDir(directory: String, actionId: String = this.actionId) {
    val dir = myFixture.tempDirFixture.findOrCreateDir(directory)
    val context = createActionContext(dir)
    myFixture.performEditorAction(actionId, TestActionEvent.createTestEvent(context))
  }

  protected fun createActionContext(file: VirtualFile? = null) = SimpleDataContext.builder()
    .add(CommonDataKeys.PROJECT, project)
    .let { if (file != null) it.add(CommonDataKeys.VIRTUAL_FILE, file) else it }
    .build()

  protected fun isProjectViewOpenedInEditor(): Boolean {
    val currentFile = FileEditorManager
      .getInstance(project)
      .currentFile
    return checkNotNull(currentFile) == myFixture.tempDirFixture.getFile(".user.bazelproject")
  }
}
