package org.jetbrains.bazel.fastbuild

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bsp.protocol.JvmToolchainInfo

class ToolchainInfoSyncHook : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.fastBuildEnabled

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val service = JvmToolchainInfoService.getInstance(environment.project)

    // clear per-target cache to ensure fresh toolchain info after sync
    // this is important in case javac configuration has changed
    logger.info("Clearing previously saved toolchains")
    service.clearPerTargetCache()
  }

  @State(
    name = "JvmToolchainInfoService",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
    reportStatistic = true,
  )
  @Service(Service.Level.PROJECT)
  class JvmToolchainInfoService : PersistentStateComponent<JvmToolchainInfoService.State> {
    private val perTargetToolchainInfo = mutableMapOf<Label, JvmToolchainInfo>()

    fun getJvmToolchainInfo(target: Label): JvmToolchainInfo? = perTargetToolchainInfo[target]

    fun setJvmToolchainInfo(toolchainInfo: JvmToolchainInfo, target: Label) {
      perTargetToolchainInfo[target] = toolchainInfo
    }

    fun clearPerTargetCache() {
      perTargetToolchainInfo.clear()
    }

    fun getOrQueryJvmToolchainInfo(project: Project, target: Label): JvmToolchainInfo {
      // first try to get cached toolchain info
      val cachedInfo = getJvmToolchainInfo(target)
      if (cachedInfo != null) {
        return cachedInfo
      }

      // query the server for per-target toolchain info
      val toolchainInfo =
        runBlocking {
          project.connection.runWithServer { server ->
            server.jvmToolchainInfoForTarget(target)
          }
        }

      // cache the result
      setJvmToolchainInfo(toolchainInfo, target)
      return toolchainInfo
    }

    override fun getState(): State? =
      State(
        perTargetToolchains =
          perTargetToolchainInfo.mapValues { (_, toolchainInfo) ->
            ToolchainInfoState(
              java_home = toolchainInfo.java_home,
              toolchain_path = toolchainInfo.toolchain_path,
              jvm_opts = toolchainInfo.jvm_opts,
            )
          },
      )

    override fun loadState(state: JvmToolchainInfoService.State) {
      logger.info("Started load of previously saved toolchains")
      this.perTargetToolchainInfo.clear()
      this.perTargetToolchainInfo.putAll(
        state.perTargetToolchains.mapValues { (_, toolchainState) ->
          JvmToolchainInfo(
            java_home = toolchainState.java_home,
            toolchain_path = toolchainState.toolchain_path,
            jvm_opts = toolchainState.jvm_opts,
          )
        },
      )
      logger.info("Finished load of previously saved toolchains")
    }

    companion object {
      fun getInstance(project: Project) = project.service<JvmToolchainInfoService>()
    }

    @Suppress("PropertyName")
    data class ToolchainInfoState(
      var java_home: String = "",
      var toolchain_path: String = "",
      var jvm_opts: List<String> = emptyList(),
    )

    data class State(var perTargetToolchains: Map<Label, ToolchainInfoState> = emptyMap())
  }

  companion object {
    private val logger = Logger.getInstance(ToolchainInfoSyncHook::class.java)
  }
}
