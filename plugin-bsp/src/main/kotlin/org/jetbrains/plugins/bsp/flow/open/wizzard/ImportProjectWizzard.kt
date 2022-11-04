package org.jetbrains.plugins.bsp.flow.open.wizzard

import com.intellij.ide.wizard.AbstractWizard
import com.intellij.ide.wizard.StepAdapter
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import org.jetbrains.plugins.bsp.config.ProjectPropertiesService
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import javax.swing.JComponent

public abstract class ImportProjectWizzardStep : StepAdapter() {

  protected abstract val panel: DialogPanel

  override fun getComponent(): JComponent = panel

  override fun _commit(finishChosen: Boolean) {
    panel.apply()
    commit(finishChosen)
  }

  protected open fun commit(finishChosen: Boolean) {}
}

public class ImportProjectWizzard(
  project: Project,
  bspConnectionDetailsGeneratorProvider: BspConnectionDetailsGeneratorProvider
) : AbstractWizard<ImportProjectWizzardStep>("Import Project via BSP", project) {

  public val connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>

  init {
    val projectPropertiesService = ProjectPropertiesService.getInstance(project)
    val projectProperties = projectPropertiesService.projectProperties
    val firstStep = ChooseConnectionFileOrNewConnectionStep(
      projectProperties.projectRootDir,
      // TODO it will be changed
      bspConnectionDetailsGeneratorProvider.firstGeneratorTEMPORARY() != null
    )
    connectionFileOrNewConnectionProperty = firstStep.connectionFileOrNewConnectionProperty
    addStep(firstStep)

    val externalSteps = bspConnectionDetailsGeneratorProvider.calulateWizzardSteps(
      bspConnectionDetailsGeneratorProvider.firstGeneratorTEMPORARY()!!,
      connectionFileOrNewConnectionProperty
    )
    externalSteps.forEach { addStep(it) }

    init()
  }

  override fun getHelpID(): String =
    "TODO"
}
