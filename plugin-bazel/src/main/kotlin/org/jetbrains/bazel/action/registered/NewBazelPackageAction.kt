package org.jetbrains.bazel.action.registered
import com.intellij.ide.IdeView
import com.intellij.ide.actions.CreateDirectoryOrPackageHandler
import com.intellij.ide.actions.OpenFileAction
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage

class NewBazelPackageAction : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    val project = e.project
    val presentation: Presentation = e.presentation
    val view = e.getData(LangDataKeys.IDE_VIEW)
    if (project == null || view == null || view.directories.isEmpty()) {
      presentation.isEnabledAndVisible = false
      return
    }
    presentation.isEnabledAndVisible = true
    val buildSystem: String = BazelPluginConstants.BAZEL_DISPLAY_NAME
    presentation.text = String.format("%s Package", buildSystem)
    presentation.description = String.format("Create a new %s package", buildSystem)
    presentation.icon = BazelPluginIcons.bazelDirectory
  }

  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    val view = event.getData<IdeView>(LangDataKeys.IDE_VIEW) ?: return
    val directory = DirectoryChooserUtil.getOrChooseDirectory(view) ?: return
    val validator: CreateDirectoryOrPackageHandler =
      object : CreateDirectoryOrPackageHandler(project, directory, false, ".") {
        override fun createDirectories(subDirName: String) {
          super.createDirectories(subDirName)
          val element = createdElement
          if (element is PsiDirectory) {
            createBuildFile(project, element)
          }
        }
      }
    Messages.showInputDialog(
      project,
      "Enter new package name:",
      String.format("New %s Package", BazelPluginConstants.BAZEL_DISPLAY_NAME),
      Messages.getQuestionIcon(),
      "",
      validator,
    )
    val newDir = validator.createdElement as PsiDirectory?
    if (newDir != null) {
      val buildFile = findBuildFile(project, newDir)
      if (buildFile != null) {
        view.selectElement(buildFile)
        OpenFileAction.openFile(buildFile.viewProvider.virtualFile, project)
      }
    }
  }

  private fun findBuildFile(project: Project, parent: PsiDirectory): PsiFile? {
    for (filename in BazelPluginConstants.BUILD_FILE_NAMES) {
      val vf = parent.virtualFile.findChild(filename)
      if (vf != null) {
        return PsiManager.getInstance(project).findFile(vf)
      }
    }
    return null
  }

  private fun createBuildFile(project: Project, parent: PsiDirectory) {
    val filename = BazelPluginConstants.BUILD_FILE_NAMES[0]
    val file =
      PsiFileFactory
        .getInstance(project)
        .createFileFromText(filename, StarlarkLanguage, "")
    parent.add(file)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
