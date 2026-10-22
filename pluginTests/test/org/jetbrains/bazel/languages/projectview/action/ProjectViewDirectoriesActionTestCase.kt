package org.jetbrains.bazel.languages.projectview.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.runInEdtAndGet
import org.intellij.lang.annotations.Language
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.directories
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.projectDirectories
import org.jetbrains.bazel.test.framework.testproject.useProjectView
import kotlin.io.path.pathString

abstract class ProjectViewDirectoriesActionTestCase(
  protected val actionId: String,
) {
  protected val myFixture by bazelCodeInsightFixture()
  protected val testBazel get() = myFixture.testBazel

  protected val project: Project get() = myFixture.project

  protected val bazelProjectView
    get() = runInEdtAndGet {
      runWithModalProgressBlocking(project, "Getting project view") {
        val service = project.service<ProjectViewService>()
        service.forceReparseCurrentProjectViewFiles()
        service.projectView
      }
    }

  protected val action: AnAction get() = ActionManager.getInstance().getAction(actionId)

  protected fun testPresentationOn(context: DataContext): Presentation {
    val event = TestActionEvent.createTestEvent(context)
    ActionUtil.updateAction(action, event)
    return event.presentation
  }

  protected fun useProjectView(@Language("projectview") content: String) {
    val projectView = myFixture.tempDirFixture.createFile(".user.bazelproject", content)
    testBazel.useProjectView(projectView.toNioPath())
    // initializes editor with some file
    val file = myFixture.createFile("Foo.kt", "")
    myFixture.openFileInEditor(file)
    val (included, excluded) = bazelProjectView.directories.partition { it.isIncluded() }
    for (entry in included + excluded) myFixture.tempDirFixture.findOrCreateDir(entry.value.pathString)
    testBazel.projectDirectories {
      include(*included.map { it.value.pathString }.toTypedArray())
      exclude(*excluded.map { it.value.pathString }.toTypedArray())
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
