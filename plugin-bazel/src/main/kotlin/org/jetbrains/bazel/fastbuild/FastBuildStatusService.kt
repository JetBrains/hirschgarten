package org.jetbrains.bazel.fastbuild

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class FastBuildStatusService(private val project: Project) {
  fun startFastBuild(status: FastBuildStatus) {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildStarted(status)
  }

  fun finishFastBuild(status: FastBuildStatus) {
    project.messageBus.syncPublisher(FastBuildStatusListener.TOPIC).fastBuildFinished(status)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): FastBuildStatusService = project.service<FastBuildStatusService>()
  }
}
