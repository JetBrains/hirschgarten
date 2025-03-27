package org.jetbrains.bazel.sync.status

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

class BuildAlreadyInProgressException : IllegalStateException()

@Service(Service.Level.PROJECT)
class BuildStatusService(private val project: Project) {
  private var _isBuildInProgress: Boolean = false

  val isBuildInProgress: Boolean
    @Synchronized get() = _isBuildInProgress

  @Synchronized
  fun startBuild() {
    if (_isBuildInProgress) throw BuildAlreadyInProgressException()
    _isBuildInProgress = true
  }

  @Synchronized
  fun finishBuild() {
    _isBuildInProgress = false
  }

  suspend fun <T> withBuildInProgress(body: suspend () -> T): T {
    startBuild()
    try {
      return body.invoke()
    } finally {
      finishBuild()
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BuildStatusService = project.getService(BuildStatusService::class.java)
  }
}

fun Project.isBuildInProgress() = BuildStatusService.getInstance(this).isBuildInProgress
