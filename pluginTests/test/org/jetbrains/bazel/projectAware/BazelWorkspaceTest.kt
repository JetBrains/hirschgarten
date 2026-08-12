package org.jetbrains.bazel.projectAware

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class BazelWorkspaceTest : WorkspaceModelBaseTest() {
  @Test
  fun `initialize registers and activates the project aware`() {
    val workspace = BazelWorkspace(project)
    Disposer.register(disposable, workspace)

    runBlocking { workspace.initialize() }

    activatedProjectIds() shouldContain workspace.projectId
  }

  @Test
  fun `disposing the workspace removes the registration`() {
    val workspace = BazelWorkspace(project)
    runBlocking { workspace.initialize() }

    Disposer.dispose(workspace)

    activatedProjectIds() shouldNotContain workspace.projectId
  }

  @Test
  fun `initialize after disposal registers nothing and does not throw`() {
    val workspace = BazelWorkspace(project)
    Disposer.dispose(workspace)

    shouldNotThrowAny {
      runBlocking { workspace.initialize() }
    }

    activatedProjectIds() shouldNotContain workspace.projectId
  }

  @Test
  fun `get top-level setting files`() {
    prepareFiles()
    val workspace = BazelWorkspace(project)

    val result = shouldNotThrowAny { computeSettingsFiles(workspace) }

    val fileNames = result.map { Path.of(it).fileName.toString() }
    fileNames shouldContainAll listOf("MODULE.bazel", ".bazelrc")
  }

  @Test
  fun `should cache results`() {
    prepareFiles()
    val workspace = BazelWorkspace(project)
    val syncCache = SyncCache.getInstance(project)

    syncCache.isAlreadyComputed(workspace.cachedBazelFiles).shouldBeFalse()
    computeSettingsFiles(workspace)
    syncCache.isAlreadyComputed(workspace.cachedBazelFiles).shouldBeTrue()
  }

  @Test
  fun `should get cancelled on collision with a write action`() {
    prepareFiles()
    val workspace = BazelWorkspace(project)

    shouldThrow<CancellationException> {
      runInBackgroundWithWriteLockTaken { workspace.settingsFiles }
    }
  }

  @Test
  fun `should not cache anything when failed`() {
    prepareFiles()
    val workspace = BazelWorkspace(project)
    val syncCache = SyncCache.getInstance(project)

    syncCache.isAlreadyComputed(workspace.cachedBazelFiles).shouldBeFalse()
    shouldThrow<CancellationException> {
      runInBackgroundWithWriteLockTaken { workspace.settingsFiles }
    }
    syncCache.isAlreadyComputed(workspace.cachedBazelFiles).shouldBeFalse()
  }

  @Test
  fun `subscribe stops delivering events after parent disposal`() {
    val workspace = BazelWorkspace(project)
    val parentDisposable = Disposer.newDisposable()
    var reloadStartCount = 0
    workspace.subscribe(
      object : ExternalSystemProjectListener {
        override fun onProjectReloadStart() {
          reloadStartCount++
        }
      },
      parentDisposable,
    )
    val publisher = project.messageBus.syncPublisher(SyncStatusListener.TOPIC)

    publisher.syncStarted()
    reloadStartCount shouldBe 1

    Disposer.dispose(parentDisposable)
    publisher.syncStarted()
    reloadStartCount shouldBe 1
  }

  private fun activatedProjectIds(): Set<ExternalSystemProjectId> = AutoImportProjectTracker.getInstance(project).getActivatedProjects()

  private fun prepareFiles() {
    project.rootDir.apply {
      createFile("MODULE.bazel")
      createFile(".bazelrc")
    }
  }

  private fun computeSettingsFiles(workspace: BazelWorkspace): Set<String> =
    ReadAction.nonBlocking<Set<String>> { workspace.settingsFiles }.executeSynchronously()

  private fun <T : Any> runInBackgroundWithWriteLockTaken(action: () -> T) =
    runTestWriteAction {
      withContext(Dispatchers.Default) { action() }
    }

  private fun VirtualFile.createFile(name: String): VirtualFile {
    if (!this.isDirectory) error("Can't create a file in a non-directory file")
    return runTestWriteAction {
      this.createChildData(this, name)
    }
  }
}
