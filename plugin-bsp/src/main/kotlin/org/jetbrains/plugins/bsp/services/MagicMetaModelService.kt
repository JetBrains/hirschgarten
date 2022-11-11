package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.DefaultMagicMetaModelState
import org.jetbrains.magicmetamodel.impl.MagicMetaModelImpl

@State(
  name = "MagicMetaModelService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true
)
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
    )

    return MagicMetaModel.create(magicMetaModelProjectConfig, emptyProjectDetails)
  }

  public fun initializeMagicModel(projectDetails: ProjectDetails) {
    val magicMetaModelProjectConfig = calculateProjectConfig(project)

    // TODO it should be init!
    value.clear()
    value = MagicMetaModel.create(magicMetaModelProjectConfig, projectDetails)
  }

  override fun getState(): DefaultMagicMetaModelState =
    value.toState()

  override fun loadState(state: DefaultMagicMetaModelState) {
    value = MagicMetaModel.fromState(state, calculateProjectConfig(project))
  }

  private fun calculateProjectConfig(project: Project): MagicMetaModelProjectConfig {
    val workspaceModel = WorkspaceModel.getInstance(project)
    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)

    return MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager)
  }

  public companion object {
    public fun getInstance(project: Project): MagicMetaModelService =
      project.getService(MagicMetaModelService::class.java)
  }
}
