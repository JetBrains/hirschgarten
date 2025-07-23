package org.jetbrains.bazel.fastbuild

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class FastBuildStatusService(private val project: Project) {
  fun startFastBuild() {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildStarted()
  }

  fun finishFastBuild(fastBuildStatus: FastBuildStatus) {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildFinished(fastBuildStatus)
  }

  fun startFastBuildTarget() {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildTargetStarted()
  }

  fun finishFastBuildTarget(fastBuildTargetStatus: FastBuildTargetStatus) {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildTargetFinished(fastBuildTargetStatus)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): FastBuildStatusService = project.service<FastBuildStatusService>()
  }
}
