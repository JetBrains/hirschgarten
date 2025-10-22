package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.toBazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.toBazelVersionStringLiteral

@Service(Service.Level.PROJECT)
@State(name = "BazelVersionCache", storages = [Storage(StoragePathMacros.CACHE_FILE)])
class BazelVersionCheckerService(private val project: Project) : PersistentStateComponent<BazelVersionCheckerService.State> {
  class State : BaseState() {
    var resolverId by string(null)
    var currentBazelVersion by string(null)
    var latestBazelVersion by string(null)
  }

  private var resolverId: String? = null

  var latestBazelVersion: BazelVersionLiteral? = null
    private set

  var currentBazelVersion: BazelVersionLiteral? = null
    private set

  override fun getState(): BazelVersionCheckerService.State =
    State().also {
      it.resolverId = resolverId
      it.currentBazelVersion = currentBazelVersion?.toBazelVersionStringLiteral()
      it.latestBazelVersion = latestBazelVersion?.toBazelVersionStringLiteral()
    }

  override fun loadState(state: BazelVersionCheckerService.State) {
    resolverId = state.resolverId
    currentBazelVersion = state.currentBazelVersion?.toBazelVersionLiteral()
    latestBazelVersion = state.latestBazelVersion?.toBazelVersionLiteral()
  }

  suspend fun refreshLatestBazelVersion(project: Project, currentVersion: BazelVersionLiteral?) {
    if (resolverId.isNullOrBlank()) {
      resolverId = BazelVersionResolver.ep.extensionList
        .firstOrNull()
        ?.id ?: return
    }
    val resolver =
      BazelVersionResolver.ep.extensionList
        .firstOrNull { it.id == state.resolverId } ?: return
    currentBazelVersion = currentVersion
    latestBazelVersion = resolver.resolveLatestBazelVersion(project, currentVersion) ?: return
  }

  /**
   * update the current version if it is different from the resolved version.
   * @return true if the version was updated, false otherwise
   */
  fun updateCurrentVersion(): Boolean {
    val projectPath = project.rootDir.toNioPath()
    val resolvedVersion = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(projectPath)
    return if (currentBazelVersion != resolvedVersion) {
      currentBazelVersion = resolvedVersion
      true
    } else {
      false
    }
  }
}
