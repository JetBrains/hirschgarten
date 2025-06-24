package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.toBazelVersionLiteral

@Service(Service.Level.PROJECT)
@State(name = "BazelVersionCache", storages = [Storage("bazelVersionCache.xml")])
class BazelVersionCheckerService :
  SimplePersistentStateComponent<BazelVersionCheckerService.State>(State()) {
  class State : BaseState() {
    var resolverId by string(null)

    var bazelVersion by string(null)
  }

  fun getLatestBazelVersion(): BazelVersionLiteral? {
    return state.bazelVersion?.toBazelVersionLiteral()
  }

  suspend fun refreshLatestBazelVersion(project: Project, currentVersion: BazelVersionLiteral?) {
    if (state.resolverId.isNullOrBlank()) {
      state.resolverId = BazelVersionResolver.ep.extensionList.firstOrNull()?.id ?: return
    }
    val resolver = BazelVersionResolver.ep.extensionList
      .firstOrNull { it.id == state.resolverId } ?: return
    state.bazelVersion = resolver.resolveLatestBazelVersion(project, currentVersion) ?: return
  }
}
