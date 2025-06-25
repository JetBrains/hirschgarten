package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.toBazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.toBazelVersionStringLiteral

@Service(Service.Level.PROJECT)
@State(name = "BazelVersionCache", storages = [Storage("bazelVersionCache.xml")])
class BazelVersionCheckerService :
  PersistentStateComponent<BazelVersionCheckerService.State> {
  class State : BaseState() {
    var resolverId by string(null)
    var bazelVersion by string(null)
  }

  private var resolverId: String? = null
  private var bazelVersion: BazelVersionLiteral? = null

  val latestBazelVersion: BazelVersionLiteral?
    get () = bazelVersion

  override fun getState(): BazelVersionCheckerService.State = State().also {
    it.resolverId = resolverId
    it.bazelVersion = bazelVersion?.toBazelVersionStringLiteral()
  }

  override fun loadState(state: BazelVersionCheckerService.State) {
    resolverId = state.resolverId
    bazelVersion = state.bazelVersion?.toBazelVersionLiteral()
  }

  suspend fun refreshLatestBazelVersion(project: Project, currentVersion: BazelVersionLiteral?) {
    if (resolverId.isNullOrBlank()) {
      resolverId = BazelVersionResolver.ep.extensionList.firstOrNull()?.id ?: return
    }
    val resolver = BazelVersionResolver.ep.extensionList
      .firstOrNull { it.id == state.resolverId } ?: return
    bazelVersion = resolver.resolveLatestBazelVersion(project, currentVersion) ?: return
  }
}
