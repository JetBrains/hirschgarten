package org.jetbrains.bazel.server.sync

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bazel.server.model.BazelSyncProject
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bsp.protocol.TaskId
import java.util.concurrent.atomic.AtomicReference

class BazelSyncProjectProvider(
  private val project: Project,
  private val projectResolver: ProjectResolver,
  private val firstPhaseProjectResolver: FirstPhaseProjectResolver)
{
  suspend fun refreshAndGet(build: Boolean, taskId: TaskId): AspectSyncProject {
    val storage = BazelSyncProjectStorage.getInstance(project)
    return storage.calc {
      projectResolver.resolve(build = build, null, storage.get() as? PhasedSyncProject, taskId)
    }
  }

  suspend fun updateAndGet(targetsToSync: List<Label>, taskId: TaskId): AspectSyncProject {
    val storage = BazelSyncProjectStorage.getInstance(project)
    return storage.calc {
      val next = projectResolver.resolve(build = false, targetsToSync, storage.get() as? PhasedSyncProject, taskId)
      (storage.get() as? AspectSyncProject)?.plus(next) ?: next
    }
  }

  suspend fun getOrLoad(taskId: TaskId): BazelSyncProject {
    val storage = BazelSyncProjectStorage.getInstance(project)
    return storage.get() ?: refreshAndGet(build = false, taskId)
  }

  suspend fun bazelQueryRefreshAndGet(taskId: TaskId): PhasedSyncProject {
    val storage = BazelSyncProjectStorage.getInstance(project)
    return storage.calc {
      firstPhaseProjectResolver.resolve(taskId)
    }
  }
}

@Service(Service.Level.PROJECT)
class BazelSyncProjectStorage(private val project: Project, private val cs: CoroutineScope) {
  private val syncProject = AtomicReference<BazelSyncProject?>(null)
  private val projectMutex = Mutex()

  fun get() = syncProject.get()

  suspend fun <T : BazelSyncProject> calc(block: suspend () -> T): T = projectMutex.withLock {
    block().also { syncProject.set(it) }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelSyncProjectStorage = project.getService(BazelSyncProjectStorage::class.java)
  }
}
