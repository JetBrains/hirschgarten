package org.jetbrains.bazel.flow.modify

import com.intellij.ide.IdeBundle
import com.intellij.ide.IdeView
import com.intellij.ide.actions.JavaCreateDirectoryOrPackageAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.storage.entities
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.common.timeoutRunBlocking
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class BazelCreateJavaClassActionAvailabilityOverrideTest {
  private val fixture by bazelCodeInsightFixture()

  @Test
  fun `creating java and kotlin files and packages should be available`() = timeoutRunBlocking {
    val project = fixture.project
    val workspaceModel = project.workspaceModel
    writeAction {
      workspaceModel.updateProjectModel("remove the ContentRoot in the project root as Bazel projects don't have it") { storage ->
        storage.entities<ContentRootEntity>().forEach { storage.removeEntity(it) }
      }
    }

    val psiDirectory = readAction { PsiManager.getInstance(project).findDirectory(project.rootDir)!! }
    val context = SimpleDataContext.builder().add(
      LangDataKeys.IDE_VIEW,
      object : IdeView {
        override fun getDirectories(): Array<out PsiDirectory> = arrayOf(psiDirectory)
        override fun getOrChooseDirectory(): PsiDirectory = psiDirectory
      },
    ).add(LangDataKeys.PROJECT, project).build()
    val actionManager = ActionManager.getInstance()
    val actions = listOf(
      actionManager.getAction("NewClass")!!,
      actionManager.getAction("Kotlin.NewFile")!!,
      actionManager.getAction("NewPackageInfo")!!,
      actionManager.getAction("NewDir")!!,
    )

    for (action in actions) {
      val actionEvent = TestActionEvent.createTestEvent(context)
      readAction { action.update(actionEvent) }
      assertTrue(actionEvent.presentation.isEnabledAndVisible, "Action ${action.templateText} should be available")
    }

    // Java's NewDir action is replaced by the Kotlin plugin normally, but it must work when the Kotlin plugin is disabled.
    for (newDirAction in listOf(actionManager.getAction("NewDir")!!, JavaCreateDirectoryOrPackageAction())) {
      val newDirEvent = TestActionEvent.createTestEvent(context)
      readAction { newDirAction.update(newDirEvent) }
      assertEquals(IdeBundle.message("action.package"), newDirEvent.presentation.text, "${newDirAction.javaClass.name} should show a package")
    }
  }
}
