package org.jetbrains.plugins.bsp.impl.server.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bsp.protocol.utils.parseBspConnectionDetails
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.impl.flow.open.wizard.ImportProjectWizard

public class DefaultConnectionDetailsProviderExtension : ConnectionDetailsProviderExtension {
  override val buildToolId: BuildToolId = bspBuildToolId

  override suspend fun onFirstOpening(project: Project, projectPath: VirtualFile): Boolean {
    if (project.stateService.connectionFile != null) return true
    val wizard = withContext(Dispatchers.EDT) { ImportProjectWizard(project) }
    Disposer.register(project.stateService, wizard.disposable)

    val wizardResult = withContext(Dispatchers.EDT) { wizard.showAndGet() }

    if (wizardResult) {
      project.stateService.connectionFile = wizard.connectionFile.get()
    }

    return wizardResult
  }

  override fun provideNewConnectionDetails(project: Project, currentConnectionDetails: BspConnectionDetails?): BspConnectionDetails? {
    val connectionDetailsFromFile =
      project.stateService.connectionFile?.parseBspConnectionDetails()
        ?: error("Cannot parse connection details from connection file. Please reimport the project.")

    return connectionDetailsFromFile.takeIf { it != currentConnectionDetails }
  }
}

internal data class BspConnectionDetailsState(
  var name: String? = null,
  var argv: List<String> = listOf(),
  var version: String? = null,
  var bspVersion: String? = null,
  var languages: List<String> = listOf(),
)

internal data class DefaultConnectionDetailsProviderState(
  var connectionFile: String? = null,
  var bspConnectionDetails: BspConnectionDetailsState? = null,
)

@State(
  name = "DefaultConnectionDetailsProviderExtensionService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class DefaultConnectionDetailsProviderExtensionService :
  PersistentStateComponent<DefaultConnectionDetailsProviderState>,
  Disposable {
  var connectionFile: VirtualFile? = null

  override fun getState(): DefaultConnectionDetailsProviderState =
    DefaultConnectionDetailsProviderState(
      connectionFile = connectionFile?.url,
    )

  override fun loadState(state: DefaultConnectionDetailsProviderState) {
    val virtualFileManager = VirtualFileManager.getInstance()
    connectionFile = state.connectionFile?.let { virtualFileManager.findFileByUrl(it) }
  }

  override fun dispose() {}

  companion object {
    @JvmStatic
    fun getInstance(project: Project): DefaultConnectionDetailsProviderExtensionService =
      project.getService(DefaultConnectionDetailsProviderExtensionService::class.java)
  }
}

internal val Project.stateService: DefaultConnectionDetailsProviderExtensionService
  get() = DefaultConnectionDetailsProviderExtensionService.getInstance(this)
