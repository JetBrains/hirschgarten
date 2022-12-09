package org.jetbrains.plugins.bsp.flow.open.wizard

import com.intellij.ide.wizard.AbstractWizard
import com.intellij.ide.wizard.StepAdapter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import org.jetbrains.plugins.bsp.config.ProjectPropertiesService
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import javax.swing.JComponent

public abstract class ImportProjectWizardStep : StepAdapter() {

  protected abstract val panel: DialogPanel

  override fun getComponent(): JComponent = panel

  override fun _commit(finishChosen: Boolean) {
    panel.apply()
    commit(finishChosen)
  }

  protected open fun commit(finishChosen: Boolean) {}
}

public class ImportProjectWizard(
  private val project: Project,
  bspConnectionDetailsGeneratorProvider: BspConnectionDetailsGeneratorProvider
) : AbstractWizard<ImportProjectWizardStep>("Import Project via BSP", project) {

  public val connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>

  init {
    val projectProperties = ProjectPropertiesService.getInstance(project).value
    val firstStep = ChooseConnectionFileOrNewConnectionStep(
      projectProperties.projectRootDir,
      // TODO it will be changed
      bspConnectionDetailsGeneratorProvider.firstGeneratorTEMPORARY() != null
    )
    connectionFileOrNewConnectionProperty = firstStep.connectionFileOrNewConnectionProperty
    addStep(firstStep)

    val externalSteps = bspConnectionDetailsGeneratorProvider.calculateWizardSteps(
      bspConnectionDetailsGeneratorProvider.firstGeneratorTEMPORARY()!!,
      connectionFileOrNewConnectionProperty
    )
    externalSteps.forEach { addStep(it) }

    init()
  }

  override fun doCancelAction() {
    super.doCancelAction()

    ProjectManager.getInstance().closeAndDispose(project)
  }

  override fun getHelpID(): String =
    "TODO"
}
