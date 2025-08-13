package org.jetbrains.bazel.languages.starlark.bazel.modules

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "BazelModuleRegistry", storages = [Storage("bazelModuleRegistry.xml")])
class BazelModuleRegistryService(private val project: Project) : PersistentStateComponent<BazelModuleRegistryService.State> {
  class State : BaseState() {
    var resolverId by string(null)
  }

  private var resolverId: String? = null
  private var activeResolver: BazelModuleResolver? = null

  private fun findResolver(): BazelModuleResolver? {
    if (activeResolver != null && activeResolver?.id == resolverId) {
      return activeResolver
    }
    if (resolverId.isNullOrBlank()) {
      resolverId = BazelModuleResolver.EP_NAME.extensionList.firstOrNull()?.id
    }
    activeResolver = BazelModuleResolver.EP_NAME.extensionList.find { it.id == resolverId }
    return activeResolver
  }

  suspend fun getModuleNames(): List<String> =
    findResolver()?.getModuleNames(project) ?: emptyList()

  suspend fun getModuleVersions(moduleName: String): List<String> =
    findResolver()?.getModuleVersions(project, moduleName) ?: emptyList()

  suspend fun refreshModuleNames() {
    findResolver()?.refreshModuleNames(project)
  }

  fun clearCache() {
    findResolver()?.clearCache(project)
  }

  fun getCachedModuleNames(): List<String> =
    findResolver()?.getCachedModuleNames(project) ?: emptyList()

  fun getCachedModuleVersions(moduleName: String): List<String> =
    findResolver()?.getCachedModuleVersions(project, moduleName) ?: emptyList()

  override fun getState(): State = State().also { it.resolverId = resolverId }

  override fun loadState(state: State) {
    resolverId = state.resolverId
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelModuleRegistryService = project.service()
  }
}
