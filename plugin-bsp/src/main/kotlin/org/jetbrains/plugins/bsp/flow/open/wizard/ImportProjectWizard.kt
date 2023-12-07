package org.jetbrains.plugins.bsp.flow.open.wizard

import com.intellij.ide.wizard.AbstractWizard
import com.intellij.ide.wizard.StepAdapter
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.rootDir
import javax.swing.JComponent

internal abstract class ImportProjectWizardStep : StepAdapter() {
  protected abstract val panel: DialogPanel

  override fun getComponent(): JComponent = panel

  override fun _commit(finishChosen: Boolean) {
    panel.apply()
    commit(finishChosen)
  }

  protected open fun commit(finishChosen: Boolean) {}
}

internal class ImportProjectWizard(
  project: Project,
) : AbstractWizard<ImportProjectWizardStep>(BspPluginBundle.message("wizard.import.project.title"), project) {
  val connectionFile: ObservableProperty<VirtualFile>

  init {
    val firstStep = ChooseConnectionFileStep(project.rootDir)
    connectionFile = firstStep.connectionFile

    addStep(firstStep)

    init()
  }

  override fun showAndGet(): Boolean =
    if (isModal) {
      super.showAndGet()
    } else {
      true
    }

  override fun getHelpID(): String? {
    TODO("Not yet implemented")
  }
}
