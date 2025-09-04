package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmMainClass
import org.jetbrains.bsp.protocol.WorkspaceTargetClasspathQueryParams
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class RunEnvironmentProvider(private val project: Project) {
  suspend fun getJvmEnvironmentItem(target: Label): JvmEnvironmentItem? =
    project.connection.runWithServer { server ->
      val paths = server.workspaceBazelPaths()
      return@runWithServer getJvmEnvironmentItem(paths.bazelPathsResolver, server, target)
    }

  private suspend fun getJvmEnvironmentItem(
    paths: BazelPathsResolver,
    server: JoinedBuildServer,
    label: Label,
  ): JvmEnvironmentItem? {
    val classpath = server.workspaceTargetClasspathQuery(WorkspaceTargetClasspathQueryParams(label))
    val resolvedClasspath = resolveClasspath(paths, classpath.runtimeClasspath)

    val plugin = project.serviceAsync<LanguagePluginsService>()
      .getLanguagePlugin<JavaLanguagePlugin>(LanguageClass.JAVA)
    val runTargetInfo = plugin.storages.runTargetInfoStore.get(label) ?: return null
    
    return JvmEnvironmentItem(
      target = label,
      classpath = resolvedClasspath,
      jvmOptions = runTargetInfo.jvmArgsList,
      workingDirectory = paths.workspaceRoot(),
      environmentVariables = runTargetInfo.envVariablesMap,
      mainClasses = if (runTargetInfo.hasMainClass()) {
        listOf(JvmMainClass(runTargetInfo.mainClass, runTargetInfo.programArgsList))
      } else {
        emptyList()
      },
    )
  }

  private fun resolveClasspath(paths: BazelPathsResolver, classpathQueryResult: List<Path>): List<Path> =
    classpathQueryResult
      .map { paths.resolveOutput(it) }
      .filter { it.toFile().exists() } // I'm surprised this is needed, but we literally test it in e2e tests
}
