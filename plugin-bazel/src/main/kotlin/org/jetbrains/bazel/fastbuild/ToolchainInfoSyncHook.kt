package org.jetbrains.bazel.fastbuild

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bsp.protocol.JvmToolchainInfo

class ToolchainInfoSyncHook : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.fastBuildEnabled

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    environment.server.jvmToolchainInfo().also {
      JvmToolchainInfoService.getInstance(environment.project).jvmToolchainInfo = it
    }
  }

  @State(
    name = "JvmToolchainInfoService",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
    reportStatistic = true,
  )
  @Service(Service.Level.PROJECT)
  class JvmToolchainInfoService : PersistentStateComponent<JvmToolchainInfoService.State> {
    var jvmToolchainInfo: JvmToolchainInfo? = null

    override fun getState(): State? =
      jvmToolchainInfo?.let {
        State(
          java_home = it.java_home,
          toolchain_path = it.toolchain_path,
          jvm_opts = it.jvm_opts,
        )
      }

    override fun loadState(state: JvmToolchainInfoService.State) {
      this.jvmToolchainInfo =
        JvmToolchainInfo(
          java_home = state.java_home,
          toolchain_path = state.toolchain_path,
          jvm_opts = state.jvm_opts,
        )
    }

    companion object {
      fun getInstance(project: Project) = project.service<JvmToolchainInfoService>()
    }

    @Suppress("PropertyName")
    data class State(
      var java_home: String = "",
      var toolchain_path: String = "",
      var jvm_opts: List<String> = emptyList(),
    )
  }
}
