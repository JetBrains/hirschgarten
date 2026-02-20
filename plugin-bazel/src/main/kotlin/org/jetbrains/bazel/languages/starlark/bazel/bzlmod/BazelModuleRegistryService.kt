package org.jetbrains.bazel.languages.starlark.bazel.bzlmod

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.util.io.createParentDirectories
import org.h2.mvstore.MVStore
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class BazelModuleRegistryService(private val project: Project) : Disposable {
  private val store: MVStore = openStore(project.getProjectDataPath("bzlmod/module-registry.mv"))
  private val map = store.openMap<String, String>("registry")

  private var resolverId: String?
    get() = map[RESOLVER_ID_KEY]
    set(value) {
      if (value.isNullOrBlank()) { map.remove(RESOLVER_ID_KEY) }
      else { map[RESOLVER_ID_KEY] = value }
      store.commit()
    }
  private var activeResolver: BazelModuleResolver? = null

  private fun findResolver(): BazelModuleResolver? {
    if (activeResolver != null && activeResolver?.id == resolverId) {
      return activeResolver
    }
    if (resolverId.isNullOrBlank()) {
      resolverId =
        BazelModuleResolver.EP_NAME.extensionList
          .firstOrNull()
          ?.id
    }
    activeResolver = BazelModuleResolver.EP_NAME.extensionList.find { it.id == resolverId }
    return activeResolver
  }

  suspend fun getModuleNames(): List<String> = findResolver()?.getModuleNames(project) ?: emptyList()

  suspend fun getModuleVersions(moduleName: String): List<String> = findResolver()?.getModuleVersions(project, moduleName) ?: emptyList()

  suspend fun refreshModuleNames() {
    findResolver()?.refreshModuleNames(project)
  }

  override fun dispose() {
    runCatching {
      store.close()
    }.onFailure {
      Logger
        .getInstance(BazelModuleRegistryService::class.java)
        .warn("Failed to close MVStore for Bazel module registry service", it)
    }
  }

  companion object {
    private const val RESOLVER_ID_KEY = "resolverId"

    @JvmStatic
    fun getInstance(project: Project): BazelModuleRegistryService = project.service()

    /**
     * Opens an MVStore instance for the given file path.
     * Creates the parent directories if needed.
     */
    private fun openStore(path: Path): MVStore {
      path.createParentDirectories()
      return MVStore.Builder()
        .fileName(path.toAbsolutePath().toString())
        .autoCommitDisabled()
        .open()
        .also { it.setVersionsToKeep(0) }
    }
  }
}
