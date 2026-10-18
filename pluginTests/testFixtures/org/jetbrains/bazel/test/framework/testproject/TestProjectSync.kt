package org.jetbrains.bazel.test.framework.testproject

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetKey
import com.intellij.openapi.components.service
import com.intellij.testFramework.LoggedErrorProcessor
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/** Returns a target proto with [label] and [kind]. */
fun targetIdeInfo(label: String, kind: String): TargetIdeInfo =
  TargetIdeInfo
    .newBuilder()
    .setKey(TargetKey.newBuilder().setLabel(label).build())
    .setKind(kind)
    .build()

/**
 * Installs a build server whose aspect sync returns [targets]. All [targets] are root targets.
 * The function writes the protos to `target-protos` in the workspace root. Set [hasError] for a sync that ends with errors.
 */
fun TestBazelProject.aspectSyncServer(vararg targets: TargetIdeInfo, hasError: Boolean = false) {
  val protoDirectory = workspaceRoot.resolve("target-protos")
  if (targets.isNotEmpty()) {
    protoDirectory.createDirectories()
  }
  val protoPaths = targets.mapIndexed { index, target ->
    protoDirectory.resolve("target$index.intellij-info.txt").apply { writeText(target.toString()) }
  }
  buildServer(
    BuildServerMock(
      aspectSyncProject = AspectSyncProject(
        workspaceRoot = Path(""),
        bazelRelease = BazelRelease(9, 0),
        hasError = hasError,
        targetProtoPaths = protoPaths.toSet(),
        rootTargets = targets.mapTo(mutableSetOf()) { WorkspaceTargetKey(label = Label.parse(it.key.label)) },
        configurations = emptyMap(),
      ),
    ),
  )
}

/** Installs a build server that throws [failure] when the sync asks for the workspace targets. */
fun TestBazelProject.failingBuildServer(failure: Throwable) {
  buildServer(
    object : BuildServerMock() {
      override suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): AspectSyncProject = throw failure
    },
  )
}

/** Runs the project sync service with [scope] and waits for the end of the sync. */
fun TestBazelProject.runProjectSync(scope: ProjectSyncScope = ProjectSyncScope.Full(build = false, phased = false)) {
  runBlocking { project.service<ProjectSyncService>().sync(scope) }
}

/** Runs [block] and ignores the "Error syncing project" error that the log gets from a failed sync. */
fun <T> expectingSyncErrorLogged(block: () -> T): T {
  var result: T? = null
  LoggedErrorProcessor.executeWith<RuntimeException>(
    object : LoggedErrorProcessor() {
      override fun processError(category: String, message: String, details: Array<String>, t: Throwable?): Set<Action> =
        if (message.contains("Error syncing project")) Action.NONE else Action.ALL
    },
  ) {
    result = block()
  }
  @Suppress("UNCHECKED_CAST")
  return result as T
}
