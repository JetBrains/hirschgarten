package org.jetbrains.bazel.server.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.BazelSyncProject
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bsp.protocol.TaskId
import java.util.concurrent.atomic.AtomicReference

class BazelSyncProjectProvider(
  private val projectResolver: ProjectResolver,
  private val firstPhaseProjectResolver: FirstPhaseProjectResolver)
{
  private val cache = AtomicReference<BazelSyncProject?>(null)
  private val projectMutex = Mutex()

  suspend fun refreshAndGet(build: Boolean, taskId: TaskId): AspectSyncProject {
    return computeAndCache {
      projectResolver.resolve(build = build, null, cache.get() as? PhasedSyncProject, taskId)
    }
  }

  suspend fun updateAndGet(targetsToSync: List<Label>, taskId: TaskId): AspectSyncProject {
    return computeAndCache {
      val next = projectResolver.resolve(build = false, targetsToSync, cache.get() as? PhasedSyncProject, taskId)
      (cache.get() as? AspectSyncProject)?.plus(next) ?: next
    }
  }

  suspend fun getOrLoad(taskId: TaskId): BazelSyncProject {
    return cache.get() ?: refreshAndGet(build = false, taskId)
  }

  suspend fun bazelQueryRefreshAndGet(taskId: TaskId): PhasedSyncProject {
    return computeAndCache {
      firstPhaseProjectResolver.resolve(taskId)
    }
  }

  private suspend fun <T : BazelSyncProject> computeAndCache(block: suspend () -> T): T = projectMutex.withLock {
    block().also { cache.set(it) }
  }
}
