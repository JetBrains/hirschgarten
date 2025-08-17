package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmMainClass
import org.jetbrains.bsp.protocol.WorkspaceTargetClasspathQueryParams
import org.jetbrains.bsp.protocol.getJvmOrNull
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class RunEnvironmentProvider(val project: Project) {
  suspend fun getJvmEnvironmentItem(
    target: Label,
  ): JvmEnvironmentItem? {
    return project.connection.runWithServer fn@{ server ->
      val paths = server.workspaceBazelPaths()
      val data = this.project.targetUtils.getBuildTargetForLabel(target)?.data ?: return@fn null
      return@fn data.getJvmEnvironmentItem(paths.bazelPathsResolver, server, target)
    }
    //fun extractJvmEnvironmentItem(module: Module, runtimeClasspath: List<Path>): JvmEnvironmentItem? =
    //  module.javaModule?.let { javaModule ->
    //    JvmEnvironmentItem(
    //      module.label,
    //      runtimeClasspath,
    //      javaModule.jvmOps.toList(),
    //      bazelPathsResolver.workspaceRoot(),
    //      module.environmentVariables,
    //      mainClasses = javaModule.mainClass?.let { listOf(JvmMainClass(it, javaModule.args)) }.orEmpty(),
    //    )
    //  }
    //
    //return targets.mapNotNull {
    //  val module = project.findModule(it)
    //  val classpath = server.workspaceTargetClasspathQuery(WorkspaceTargetClasspathQueryParams(it))
    //  val resolvedClasspath = resolveClasspath(classpath.runtimeClasspath)
    //  module?.let { extractJvmEnvironmentItem(module, resolvedClasspath) }
    //}
  }

  private suspend fun BuildTargetData.getJvmEnvironmentItem(
    paths: BazelPathsResolver,
    server: JoinedBuildServer,
    label: Label,
  ): JvmEnvironmentItem? {
    val jvmTarget = this.getJvmOrNull() ?: return null
    val classpath = server.workspaceTargetClasspathQuery(WorkspaceTargetClasspathQueryParams(label))
    val resolvedClasspath = resolveClasspath(paths, classpath.runtimeClasspath)

    return JvmEnvironmentItem(
      target = label,
      classpath = resolvedClasspath,
      jvmOptions = jvmTarget.jvmArgs,
      workingDirectory = paths.workspaceRoot(),
      environmentVariables = jvmTarget.environmentVariables,
      mainClasses = jvmTarget.mainClass?.let { listOf(JvmMainClass(it, jvmTarget.programArgs)) }.orEmpty(),
    )
  }

  private fun resolveClasspath(paths: BazelPathsResolver, cqueryResult: List<Path>): List<Path> =
    cqueryResult
      .map { paths.resolveOutput(it) }
      .filter { it.toFile().exists() } // I'm surprised this is needed, but we literally test it in e2e tests
}
