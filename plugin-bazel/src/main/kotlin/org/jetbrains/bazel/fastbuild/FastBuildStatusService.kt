package org.jetbrains.bazel.fastbuild

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class FastBuildStatusService(private val project: Project) {
  fun startFastBuild(fastBuildStatus: FastBuildStatus) {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildStarted(fastBuildStatus)
  }

  fun finishFastBuild(fastBuildStatus: FastBuildStatus) {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildFinished(fastBuildStatus)
  }

  fun startFastBuildTarget(fastBuildTargetStatus: FastBuildTargetStatus) {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildTargetStarted(fastBuildTargetStatus)
  }

  fun finishFastBuildTarget(fastBuildTargetStatus: FastBuildTargetStatus) {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildTargetStarted(fastBuildTargetStatus)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): FastBuildStatusService = project.service<FastBuildStatusService>()
  }
}
