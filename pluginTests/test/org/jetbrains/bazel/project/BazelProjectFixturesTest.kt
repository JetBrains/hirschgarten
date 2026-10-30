package org.jetbrains.bazel.project

import com.intellij.openapi.Disposable
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.startup.StartupActivity
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.replaceService
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProjectViaProjectView
import org.jetbrains.bazel.sync.ProjectSyncCompletionResult
import org.jetbrains.bazel.sync.ProjectSyncOutcome
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bsp.protocol.TaskId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.io.path.writeText

@BazelTestApplication
@Timeout(30)
internal class BazelProjectFixturesTest {
  private val projectDir = tempPathFixture()
  private val project by projectFixture(projectDir, openAfterCreation = false)

  @TestDisposable
  lateinit var disposable: Disposable

  private val syncService = RecordingSyncService()

  @BeforeEach
  fun replaceSyncService() {
    project.replaceService(ProjectSyncService::class.java, syncService, disposable)
  }

  @ParameterizedTest
  @EnumSource(ProjectInitialization::class)
  fun `fixtures skip startup sync but allow explicit sync`(initialization: ProjectInitialization): Unit = timeoutRunBlocking {
    initialize(initialization)

    runStartupActivity()
    syncService.scopes.shouldBeEmpty()

    val scope = ProjectSyncScope.Full(build = false, phased = false)
    project.getService(ProjectSyncService::class.java).sync(scope)
    syncService.scopes shouldContainExactly listOf(scope)
  }

  @ParameterizedTest
  @EnumSource(ProjectInitialization::class)
  fun `fixtures can enable startup sync after it was disabled`(initialization: ProjectInitialization): Unit = timeoutRunBlocking {
    initialize(initialization)
    initialize(initialization, runStartupSync = true)

    runStartupActivity()
    syncService.scopes shouldHaveSize 1
  }

  private fun initialize(initialization: ProjectInitialization, runStartupSync: Boolean = false) {
    val root = projectDir.get()
    when (initialization) {
      ProjectInitialization.PATH -> initializeBazelProject(project, root, runStartupSync)
      ProjectInitialization.PROJECT_VIEW -> {
        val projectView = root.resolve(".bazelproject")
        projectView.writeText("directories:\n  .\n")
        initializeBazelProjectViaProjectView(project, projectView, runStartupSync)
      }
    }
  }

  private suspend fun runStartupActivity() {
    val activity = StartupActivity.POST_STARTUP_ACTIVITY.filterableLazySequence()
      .single { it.implementationClassName == "org.jetbrains.bazel.startup.BazelStartupActivity" }
      .instance as ProjectActivity
    activity.execute(project)
  }

  enum class ProjectInitialization { PATH, PROJECT_VIEW }
}

private class RecordingSyncService : ProjectSyncService {
  override val lastSyncTaskId: TaskId? = null
  val scopes = mutableListOf<ProjectSyncScope>()

  override suspend fun sync(scope: ProjectSyncScope): ProjectSyncOutcome {
    scopes.add(scope)
    return ProjectSyncOutcome(ProjectSyncCompletionResult.SUCCESS)
  }
}
