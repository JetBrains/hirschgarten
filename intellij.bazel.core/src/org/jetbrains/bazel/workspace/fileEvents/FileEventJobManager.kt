package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Job
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bsp.protocol.TaskGroupId
import java.util.concurrent.atomic.AtomicReference

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class FileEventJobManager(private val project: Project) {
  private val fileEventProcessingJob = AtomicReference<Job?>(null)

  @Volatile
  var syncTaskGroupId: TaskGroupId? = null

  init {
    project.messageBus.connect().subscribe(SyncStatusListener.TOPIC, StatusListener())
  }

  fun <T : Job> runFileEventsProcessing(jobStarter: () -> T): T? {
    val syncStatusService = SyncStatusService.getInstance(project)
    val job =
      syncStatusService.withSyncStatus { syncInProgress -> jobStarter.takeIf { !syncInProgress }?.invoke() } ?: return null
    setJobAndCancelPrevious(job)
    return job
  }

  private fun cancelCurrentJob() {
    setJobAndCancelPrevious(null)
  }

  private fun setJobAndCancelPrevious(job: Job?) {
    fileEventProcessingJob.getAndSet(job)?.cancel()
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): FileEventJobManager = project.service()
  }

  private inner class StatusListener : SyncStatusListener {
    override fun syncStarted() {
      val jobRunner = this@FileEventJobManager
      jobRunner.cancelCurrentJob()
    }

    override fun syncFinished(canceled: Boolean) { }
  }
}
