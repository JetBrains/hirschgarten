package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmMainClass
import org.jetbrains.bsp.protocol.WorkspaceTargetClasspathQueryParams
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class RunEnvironmentProvider(private val project: Project) {
  suspend fun getJvmEnvironmentItem(target: Label): JvmEnvironmentItem? =
    project.connection.runWithServer { server ->
      val paths = server.workspaceBazelPaths()
      return@runWithServer this.project.targetUtils
        .getBuildTargetForLabel(target)
        ?.getJvmEnvironmentItem(paths.bazelPathsResolver, server, target)
    }

  private suspend fun BuildTarget.getJvmEnvironmentItem(
    paths: BazelPathsResolver,
    server: JoinedBuildServer,
    label: Label,
  ): JvmEnvironmentItem? {
    val jvmTarget = extractJvmBuildTarget(this) ?: return null
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

  private fun resolveClasspath(paths: BazelPathsResolver, classpathQueryResult: List<Path>): List<Path> =
    classpathQueryResult
      .map { paths.resolveOutput(it) }
      .filter { it.toFile().exists() } // I'm surprised this is needed, but we literally test it in e2e tests
}
