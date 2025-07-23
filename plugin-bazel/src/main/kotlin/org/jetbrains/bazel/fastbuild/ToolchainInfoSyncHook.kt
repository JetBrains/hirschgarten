package org.jetbrains.bazel.fastbuild

import com.intellij.openapi.components.Service
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

  @Service(Service.Level.PROJECT)
  class JvmToolchainInfoService {
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
        logger.info("Using cached toolchain for ${target.targetName}")
        return cachedInfo
      }

      // query the server for per-target toolchain info
      val toolchainInfo =
        runBlocking {
          project.connection.runWithServer { server ->
            logger.info("Fetching toolchain for ${target.targetName}")
            server.jvmToolchainInfoForTarget(target)
          }
        }

      // cache the result
      logger.info("Storing toolchain for ${target.targetName}")
      setJvmToolchainInfo(toolchainInfo, target)
      return toolchainInfo
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
