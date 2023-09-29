package org.jetbrains.plugins.bsp.flow.open.wizard

import com.intellij.ide.wizard.AbstractWizard
import com.intellij.ide.wizard.StepAdapter
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import org.jetbrains.plugins.bsp.config.rootDir
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
  project: Project,
  private val bspConnectionDetailsGeneratorProvider: BspConnectionDetailsGeneratorProvider,
) : AbstractWizard<ImportProjectWizardStep>("Import Project via BSP", project) {
  public val connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
  private val firstStep: ChooseConnectionFileOrNewConnectionStep

  init {
    firstStep = ChooseConnectionFileOrNewConnectionStep(
      project.rootDir,
      bspConnectionDetailsGeneratorProvider.availableBspConnectionDetailsGenerators,
    ) { updateWizardButtonsToGeneratorSelection() }
    connectionFileOrNewConnectionProperty = firstStep.connectionFileOrNewConnectionProperty

    addStep(firstStep)
    init()

    if (firstStep.canBeSkipped()) {
      proceedToNextStep()
    }

    updateWizardButtonsToGeneratorSelection()
  }

  private fun updateWizardButtonsToGeneratorSelection() {
    doIfOnConnectionChoiceStep {
      val generatorSteps = calculateGeneratorSteps()
      val noFurtherSteps = generatorSteps.isEmpty()
      updateButtons(noFurtherSteps, canGoNext(), isFirstStep)
    }
  }

  private inline fun doIfOnConnectionChoiceStep(function: () -> Unit) {
    if (currentStepObject is ChooseConnectionFileOrNewConnectionStep) function()
  }

  private fun calculateGeneratorSteps(): List<ImportProjectWizardStep> {
    val connectionId = connectionFileOrNewConnectionProperty.get().id
    return bspConnectionDetailsGeneratorProvider.calculateWizardSteps(
      connectionId,
      connectionFileOrNewConnectionProperty,
    )
  }

  override fun createSouthPanel(): JComponent {
    val emptyStep = object : ImportProjectWizardStep() {
      override val panel: DialogPanel = DialogPanel()
    }
    mySteps.add(emptyStep)
    val panel = super.createSouthPanel()
    mySteps.remove(emptyStep)
    return panel
  }

  override fun showAndGet(): Boolean {
    if (areThereStepsToShow()) {
      super.showAndGet()
    }

    return true
  }

  private fun areThereStepsToShow(): Boolean =
    !firstStep.canBeSkipped() || stepCount > 1

  // TODO https://youtrack.jetbrains.com/issue/BAZEL-631
  override fun getHelpID(): String =
    "TODO"

  override fun proceedToNextStep() {
    doIfOnConnectionChoiceStep { addGeneratorSteps() }
    super.proceedToNextStep()
  }

  private fun addGeneratorSteps() {
    removeAllFollowingSteps()
    calculateGeneratorSteps().forEach { addStep(it) }
  }

  private fun removeAllFollowingSteps() {
    val stepsToRetain = currentStep + 1
    while (mySteps.size > stepsToRetain) {
      mySteps.removeAt(stepsToRetain)
    }
  }
}
