package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.DefaultMagicMetaModelState
import org.jetbrains.magicmetamodel.impl.MagicMetaModelImpl
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.BspBuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.BspBuildTargetClassifierProvider

@State(
  name = "MagicMetaModelService",
  storages = [Storage("magicmetamodel.xml")],
  reportStatistic = true
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

    val moduleNameProvider = obtainModuleNameProvider()
    val projectBasePath = project.rootDir.toNioPath()

    return MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager, moduleNameProvider, projectBasePath)
  }

  public fun obtainModuleNameProvider(): ModuleNameProvider? =
    obtainToolNameIfKnown(project)?.let { createModuleNameProviderForTool(it) }

  private fun obtainToolNameIfKnown(project: Project): String? =
    BspConnectionService.getInstance(project).value?.buildToolId

  private fun createModuleNameProviderForTool(toolName: String): ModuleNameProvider {
    val targetClassifier =
      BspBuildTargetClassifierProvider(toolName, BspBuildTargetClassifierExtension.extensions())
    return {
      targetClassifier.getBuildTargetPath(it)
        .joinToString(".", postfix = ".${targetClassifier.getBuildTargetName(it)}")
    }
  }

  public companion object {
    public fun getInstance(project: Project): MagicMetaModelService =
      project.getService(MagicMetaModelService::class.java)
  }
}
