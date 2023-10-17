package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.getInstance
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.DefaultMagicMetaModelState
import org.jetbrains.magicmetamodel.impl.MagicMetaModelImpl
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider

@State(
  name = "MagicMetaModelService",
  storages = [Storage("magicmetamodel.xml")],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
@ApiStatus.Internal
public class MagicMetaModelService(private val project: Project) :
  ValueServiceWhichNeedsToBeInitialized<MagicMetaModelImpl>(),
  PersistentStateComponent<DefaultMagicMetaModelState> {
  // TODO ugh again
  init {
    init(initEmptyMagicMetaModelTEMPORARY())
  }

  private fun initEmptyMagicMetaModelTEMPORARY(): MagicMetaModelImpl {
    val magicMetaModelProjectConfig = calculateProjectConfig(project)
    val emptyProjectDetails = ProjectDetails(
      targetsId = emptyList(),
      targets = emptySet(),
      sources = emptyList(),
      resources = emptyList(),
      dependenciesSources = emptyList(),
      javacOptions = emptyList(),
      pythonOptions = emptyList(),
      outputPathUris = emptyList(),
      libraries = emptyList(),
      scalacOptions = emptyList()
    )

    return MagicMetaModel.create(magicMetaModelProjectConfig, emptyProjectDetails)
  }

  public fun initializeMagicModel(projectDetails: ProjectDetails) {
    val magicMetaModelProjectConfig = calculateProjectConfig(project)

    val newMMM = MagicMetaModel.create(magicMetaModelProjectConfig, projectDetails)
    value.copyAllTargetLoadListenersTo(newMMM)

    // TODO it should be init!
    value.clear()
    value = newMMM
  }

  override fun getState(): DefaultMagicMetaModelState =
    value.toState()

  override fun loadState(state: DefaultMagicMetaModelState) {
    value = MagicMetaModel.fromState(state, calculateProjectConfig(project))
  }

  private fun calculateProjectConfig(project: Project): MagicMetaModelProjectConfig {
    val workspaceModel = WorkspaceModel.getInstance(project)
    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)

    val moduleNameProvider = project.findModuleNameProvider()
    val projectBasePath = project.rootDir.toNioPath()

    val isPythonSupportEnabled = BspFeatureFlags.isPythonSupportEnabled && pythonSdkGetterExtensionExists()

    val hasDefaultPythonInterpreter = pythonSdkGetterExtension()?.hasDetectedPythonSdk() == true

    return MagicMetaModelProjectConfig(
      workspaceModel,
      virtualFileUrlManager,
      moduleNameProvider,
      projectBasePath,
      isPythonSupportEnabled,
      hasDefaultPythonInterpreter
    )
  }

  public companion object {
    public fun getInstance(project: Project): MagicMetaModelService =
      project.getService(MagicMetaModelService::class.java)
  }
}
