package org.jetbrains.bazel.hotswap

import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.ui.HotSwapStatusListener
import com.intellij.debugger.ui.HotSwapUIImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.task.ProjectTaskContext
import org.jetbrains.bazel.buildTask.AdditionalProjectTask
import org.jetbrains.bazel.buildTask.AdditionalProjectTaskRunnerProvider
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.getModuleEntity
import org.jetbrains.bazel.workspacemodel.entities.jvmBinaryJarsEntity
import java.nio.file.Path

internal class BazelHotSwapProjectTaskRunnerProvider : AdditionalProjectTaskRunnerProvider {
  override fun createTask(project: Project, projectTaskContext: ProjectTaskContext, targetsToBuild: List<Label>): AdditionalProjectTask? {
    if (!HotSwapUtils.isHotSwapEligible(project)) return null
    return HotSwapTask(project, projectTaskContext, targetsToBuild)
  }
}

private data class HotSwapState(
  val jars: List<Path>,
  val listener: HotSwapStatusListener?,
  val oldManifest: JarFileManifest,
  val sessions: List<DebuggerSession>,
)

private class HotSwapTask(
  private val project: Project,
  private val projectTaskContext: ProjectTaskContext,
  private val targetsToBuild: List<Label>,
) : AdditionalProjectTask {
  private var hotSwapState: HotSwapState? = null

  override suspend fun preRun() {
    val jars: List<Path> = getTargetJars()

    // A workaround to get hotswap callback so we may report hotswap status back
    // even if we disable built-in hotswap (built for JPS compiler)
    val listener = projectTaskContext.getUserData(HotSwapUIImpl.HOT_SWAP_CALLBACK_KEY)
    // This key disables built-in IDEA hotswap (built for JPS compiler)
    projectTaskContext.putUserData(HotSwapUIImpl.SKIP_HOT_SWAP_KEY, true)

    val sessions = BazelHotSwapManager.getInstance(project).getCurrentDebugSessions()
    if (sessions.isEmpty()) return
    val oldManifest = JarFileManifest.build(jars, previousManifest = null)
    hotSwapState = HotSwapState(jars, listener, oldManifest, sessions)
  }

  private fun getTargetJars(): List<Path> = targetsToBuild.asSequence()
    .map { it.getModuleEntity(project) }
    .mapNotNull { it?.jvmBinaryJarsEntity }
    .flatMap { it.jars }
    .mapNotNull { it.virtualFile }
    .distinct()
    .mapNotNull { it.toNioPathOrNull() }
    .toList()

  override suspend fun postRun(result: BazelStatus) {
    val hotSwapState = this.hotSwapState ?: return
    if (result != BazelStatus.SUCCESS) {
      hotSwapState.listener?.onFailure(hotSwapState.sessions)
      return
    }
    val newManifest = JarFileManifest.build(hotSwapState.jars, previousManifest = hotSwapState.oldManifest)
    BazelHotSwapManager.getInstance(project).hotswap(
      BazelHotSwapManager.HotSwapEnvironment(
        oldManifest = hotSwapState.oldManifest,
        newManifest = newManifest,
        listener = hotSwapState.listener,
        sessions = hotSwapState.sessions,
        isAutoRun = projectTaskContext.isAutoRun,
      ),
    )
  }
}
