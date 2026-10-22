package org.jetbrains.bazel.sync.status

import com.intellij.openapi.project.Project
import com.intellij.testFramework.registerOrReplaceServiceInstance
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.bazel.sync.ProjectSyncCompletionResult
import org.jetbrains.bazel.sync.ProjectSyncOutcome
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.aspectSyncServer
import org.jetbrains.bazel.test.framework.testproject.expectingSyncErrorLogged
import org.jetbrains.bazel.test.framework.testproject.failingBuildServer
import org.jetbrains.bazel.test.framework.testproject.runProjectSync
import org.jetbrains.bazel.test.framework.testproject.targetIdeInfo
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bsp.protocol.TaskId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Guards the sync checks of the IDE Starter commands `%waitForBazelSync` and `%buildAndSync`.
 *
 * A check must fail when the sync does not end in SUCCESS or PARTIAL_SUCCESS. The project here keeps the targets of an
 * earlier sync. So a check that only counts targets passes after a failed sync, and hides the failure (BAZEL-3669).
 *
 * A project that reopens from its cache runs no sync. The wait must still pass there (BAZEL-3661).
 */
@BazelTestApplication
@DisplayName("The sync checks of the IDE Starter commands")
class SyncOutcomeChecksTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `awaitSyncAndCheckOutcome passes after a successful sync`() {
    syncWithTargets()

    runBlocking { withTimeout(TIMEOUT) { testBazel.project.awaitSyncAndCheckOutcome(SHORT_START_WINDOW) } }
  }

  @Test
  fun `awaitSyncAndCheckOutcome passes after a sync with errors that kept the targets`() {
    syncWithTargets(hasError = true)

    runBlocking { withTimeout(TIMEOUT) { testBazel.project.awaitSyncAndCheckOutcome(SHORT_START_WINDOW) } }
  }

  @Test
  fun `awaitSyncAndCheckOutcome fails after a sync that failed and left the old targets`() {
    syncWithTargets()
    syncWithFailure()
    testBazel.project.targetStorage.getTotalTargetCount() shouldBe 1

    val failure = shouldThrow<IllegalStateException> {
      runBlocking { withTimeout(TIMEOUT) { testBazel.project.awaitSyncAndCheckOutcome(SHORT_START_WINDOW) } }
    }

    failure.message shouldContain "boom while resolving workspace"
  }

  @Test
  fun `awaitSyncAndCheckOutcome passes when the project reopens from its cache and no sync runs`() {
    syncWithTargets()
    // A new status service has no sync history, as after a restart. The targets stay in the storage.
    val project = testBazel.project
    project.registerOrReplaceServiceInstance(SyncStatusService::class.java, SyncStatusService(project), testBazel.disposable)

    runBlocking { withTimeout(TIMEOUT) { project.awaitSyncAndCheckOutcome(SHORT_START_WINDOW) } }
  }

  @Test
  fun `awaitSyncAndCheckOutcome fails when a sync that starts in the window fails`() {
    syncWithTargets()
    val failure = runBlocking {
      // Start the wait at once, so that it reads the sync count before the next sync starts.
      val wait = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
        runCatching { withTimeout(TIMEOUT * 2) { testBazel.project.awaitSyncAndCheckOutcome(TIMEOUT) } }
      }

      syncWithFailure()

      wait.await().exceptionOrNull()
    }

    (failure as? IllegalStateException)?.message.orEmpty() shouldContain "boom while resolving workspace"
  }

  @Test
  fun `syncAndCheckOutcome passes when the sync succeeds`() {
    syncWithTargets()

    runBlocking { withTimeout(TIMEOUT) { testBazel.project.syncAndCheckOutcome(FULL_SYNC) } }
  }

  @Test
  fun `syncAndCheckOutcome fails when the sync fails`() {
    syncWithTargets()
    testBazel.failingBuildServer(IllegalStateException("boom while resolving workspace"))

    val failure = expectingSyncErrorLogged {
      shouldThrow<IllegalStateException> {
        runBlocking { withTimeout(TIMEOUT) { testBazel.project.syncAndCheckOutcome(FULL_SYNC) } }
      }
    }

    failure.message shouldContain "boom while resolving workspace"
  }

  @Test
  fun `syncAndCheckOutcome retries when another sync started and the call was skipped`() {
    val syncService = SkippingProjectSyncService(testBazel.project, startAnotherSync = true)
    testBazel.project.registerOrReplaceServiceInstance(ProjectSyncService::class.java, syncService, testBazel.disposable)

    runBlocking { withTimeout(TIMEOUT) { testBazel.project.syncAndCheckOutcome(FULL_SYNC) } }

    syncService.calls shouldBe 2
  }

  @Test
  fun `syncAndCheckOutcome fails when the call was skipped and no other sync started`() {
    val syncService = SkippingProjectSyncService(testBazel.project, startAnotherSync = false)
    testBazel.project.registerOrReplaceServiceInstance(ProjectSyncService::class.java, syncService, testBazel.disposable)

    val failure = shouldThrow<IllegalStateException> {
      runBlocking { withTimeout(TIMEOUT) { testBazel.project.syncAndCheckOutcome(FULL_SYNC) } }
    }

    failure.message shouldContain "SKIPPED"
    syncService.calls shouldBe 1
  }

  private fun syncWithTargets(hasError: Boolean = false) {
    testBazel.aspectSyncServer(targetIdeInfo("//app:java", "java_library"), hasError = hasError)
    testBazel.runProjectSync()
    testBazel.project.targetStorage.getTotalTargetCount() shouldBe 1
  }

  private fun syncWithFailure() {
    testBazel.failingBuildServer(IllegalStateException("boom while resolving workspace"))
    expectingSyncErrorLogged { testBazel.runProjectSync() }
  }
}

private val TIMEOUT = 60.seconds

/** The start window for a test where the sync is already over. The check waits for the whole window when no sync starts. */
private val SHORT_START_WINDOW = 100.milliseconds

private val FULL_SYNC = ProjectSyncScope.Full(build = true, phased = false)

/**
 * Skips the first call. With [startAnotherSync], another sync runs during the first call, as when a sync starts between
 * the wait and the call. Every later call succeeds.
 */
private class SkippingProjectSyncService(private val project: Project, private val startAnotherSync: Boolean) : ProjectSyncService {
  @Volatile
  var calls = 0
    private set

  override val lastSyncTaskId: TaskId? = null

  override suspend fun sync(scope: ProjectSyncScope): ProjectSyncOutcome {
    calls++
    if (calls > 1) return ProjectSyncOutcome(ProjectSyncCompletionResult.SUCCESS)

    if (startAnotherSync) {
      val statusService = SyncStatusService.getInstance(project)
      statusService.startSync()
      statusService.finishSync(ProjectSyncOutcome(ProjectSyncCompletionResult.SUCCESS))
    }
    return ProjectSyncOutcome(ProjectSyncCompletionResult.SKIPPED)
  }
}
