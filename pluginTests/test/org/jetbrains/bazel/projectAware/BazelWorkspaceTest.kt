package org.jetbrains.bazel.projectAware

import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemModificationType
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext.Event
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext.ReloadStatus
import com.intellij.openapi.util.Disposer
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test
import java.nio.file.Path

@BazelTestApplication
internal class BazelWorkspaceTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `initialize registers and activates the project aware`() {
    val workspace = BazelWorkspace(testBazel.project)
    Disposer.register(testBazel.disposable, workspace)

    runBlocking { workspace.initialize() }

    activatedProjectIds() shouldContain workspace.projectId
  }

  @Test
  fun `disposing the workspace removes the registration`() {
    val workspace = BazelWorkspace(testBazel.project)
    runBlocking { workspace.initialize() }

    Disposer.dispose(workspace)

    activatedProjectIds() shouldNotContain workspace.projectId
  }

  @Test
  fun `initialize after disposal registers nothing and does not throw`() {
    val workspace = BazelWorkspace(testBazel.project)
    Disposer.dispose(workspace)

    shouldNotThrowAny {
      runBlocking { workspace.initialize() }
    }

    activatedProjectIds() shouldNotContain workspace.projectId
  }

  @Test
  fun `setting files hold no BUILD file`() {
    prepareFiles()
    testBazel.writeFile("src/BUILD.bazel")
    testBazel.writeFile("src/BUILD")
    testBazel.writeFile("src/defs.bzl")
    val workspace = BazelWorkspace(testBazel.project)

    val result = shouldNotThrowAny { workspace.settingsFiles }

    val fileNames = result.map { Path.of(it).fileName.toString() }
    fileNames shouldNotContain "BUILD.bazel"
    fileNames shouldNotContain "BUILD"
    fileNames shouldNotContain "defs.bzl"
    // only the root configuration files and the project view file
    result.size shouldBeLessThan 10
  }

  @Test
  fun `setting files do not throw when the project view file is missing`() {
    prepareFiles()
    val workspace = BazelWorkspace(testBazel.project)

    shouldNotThrowAny { workspace.settingsFiles }
  }

  @Test
  fun `every settings file event is ignored`() {
    val workspace = BazelWorkspace(testBazel.project)

    // the plugin tracks the Bazel files itself, so the platform CRC scan must stay silent
    Event.entries.forEach { event ->
      workspace.isIgnoredSettingsFileEvent("/any/path", modificationContext(event)).shouldBeTrue()
    }
  }

  @Test
  fun `subscribe stops delivering events after parent disposal`() {
    val workspace = BazelWorkspace(testBazel.project)
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
    val publisher = testBazel.project.messageBus.syncPublisher(SyncStatusListener.TOPIC)

    publisher.syncStarted()
    reloadStartCount shouldBe 1

    Disposer.dispose(parentDisposable)
    publisher.syncStarted()
    reloadStartCount shouldBe 1
  }

  private fun activatedProjectIds(): Set<ExternalSystemProjectId> =
    AutoImportProjectTracker.getInstance(testBazel.project).getActivatedProjects()

  private fun modificationContext(event: Event): ExternalSystemSettingsFilesModificationContext =
    object : ExternalSystemSettingsFilesModificationContext {
      override val event: Event = event
      override val modificationType: ExternalSystemModificationType = ExternalSystemModificationType.INTERNAL
      override val reloadStatus: ReloadStatus = ReloadStatus.IDLE
    }

  private fun prepareFiles() {
    testBazel.writeFile(".bazelrc")
  }
}
