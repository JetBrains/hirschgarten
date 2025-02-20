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
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import javax.swing.Icon

class NewBazelPackageAction : DumbAwareAction {
  constructor() : super()

  constructor(icon: Icon?) : super(icon)

  constructor(text: String?) : super(text)

  constructor(text: String?, description: String?, icon: Icon?) : super(text, description, icon)

  override fun update(e: AnActionEvent) {
    val project = e.project
    val presentation: Presentation = e.presentation
    val view = e.getData(LangDataKeys.IDE_VIEW)
    if (project == null || view == null) {
      presentation.isEnabledAndVisible = false
      return
    }

    if (project.isBazelProject && view.directories.isNotEmpty()) {
      presentation.isEnabledAndVisible = true
    } else {
      presentation.isEnabledAndVisible = false
      return
    }
    val buildSystem: String = BazelPluginConstants.BAZEL_DISPLAY_NAME
    presentation.text = String.format("%s Package", buildSystem)
    presentation.description = String.format("Create a new %s package", buildSystem)
    presentation.icon = PlatformIcons.PACKAGE_ICON
  }

  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    val view: IdeView =
      event.getData<IdeView>(LangDataKeys.IDE_VIEW)
        ?: return
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
      val buildFile: PsiFile? = findBuildFile(project, newDir)
      if (buildFile != null) {
        view.selectElement(buildFile)
        OpenFileAction.openFile(buildFile.viewProvider.virtualFile, project)
      }
    }
  }

  private fun findBuildFile(project: Project, parent: PsiDirectory): PsiFile? {
    val filename: String = BazelPluginConstants.BUILD_FILE_NAMES[0]
    val vf = parent.virtualFile.findChild(filename)
    return if (vf != null) PsiManager.getInstance(project).findFile(vf) else null
  }

  private fun createBuildFile(project: Project, parent: PsiDirectory) {
    val filename: String = BazelPluginConstants.BUILD_FILE_NAMES[0]
    val file: PsiFile =
      PsiFileFactory
        .getInstance(project)
        .createFileFromText(filename, StarlarkLanguage, "")
    parent.add(file)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
