package org.jetbrains.bazel.action.registered
import com.intellij.ide.IdeView
import com.intellij.ide.actions.CreateDirectoryOrPackageHandler
import com.intellij.ide.actions.OpenFileAction
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiDirectory
import com.intellij.ui.DocumentAdapter
import com.intellij.util.Consumer
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent

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
    val ideView = event.getData(LangDataKeys.IDE_VIEW) ?: return
    val directory = DirectoryChooserUtil.getOrChooseDirectory(ideView) ?: return
    val validator: CreateDirectoryOrPackageHandler =
      object : CreateDirectoryOrPackageHandler(project, directory, false, ".") {
        override fun createDirectories(subDirName: String) {
          super.createDirectories(subDirName)
          val element = createdElement
          if (element is PsiDirectory) {
            createBuildFile(project, element, ideView)
          }
        }
      }

    // UI code based on CreateDirectoryOrPackageAction
    val contentPanel = NewItemSimplePopupPanel()
    val nameField = contentPanel.textField
    nameField.document.addDocumentListener(
      object : DocumentAdapter() {
        override fun textChanged(event: DocumentEvent) {
          val name = nameField.text
          validator.checkInput(name)
          val errorText = validator.getErrorText(name) ?: validator.getWarningText(name)
          // invokeLater here to make sure that the text update in the UI does not interfere with showing the error popups
          SwingUtilities.invokeLater {
            contentPanel.setError(errorText)
          }
        }
      },
    )

    val popup =
      NewItemPopupUtil.createNewItemPopup(
        "New Bazel Package",
        contentPanel,
        nameField,
      )
    contentPanel.applyAction =
      Consumer { event ->
        val name = nameField.getText()
        validator.checkInput(name)
        if (validator.getErrorText(name) == null && validator.getWarningText(name) == null && validator.canClose(name)) {
          popup.closeOk(event)
        }
      }
    popup.showCenteredInCurrentWindow(project)
  }

  private fun createBuildFile(
    project: Project,
    parent: PsiDirectory,
    ideView: IdeView,
  ) {
    val filename = BazelPluginConstants.defaultBuildFileName()
    val buildFile =
      WriteCommandAction.writeCommandAction(project).compute(
        ThrowableComputable {
          parent.createFile(filename)
        },
      )
    ideView.selectElement(buildFile)
    OpenFileAction.openFile(buildFile.viewProvider.virtualFile, project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
