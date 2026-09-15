package org.jetbrains.bazel.project

import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.startup.StartupActivity
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.replaceService
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProjectViaProjectView
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.TaskId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@Timeout(30)
internal class BazelProjectFixturesTest : MockProjectBaseTest() {
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
    assertEquals(0, syncService.scopes.size)

    val scope = ProjectSyncScope.Full(build = false, phased = false)
    project.getService(ProjectSyncService::class.java).sync(scope)
    assertEquals(listOf<ProjectSyncScope>(scope), syncService.scopes)
  }

  @ParameterizedTest
  @EnumSource(ProjectInitialization::class)
  fun `fixtures can enable startup sync after it was disabled`(initialization: ProjectInitialization): Unit = timeoutRunBlocking {
    initialize(initialization)
    initialize(initialization, runStartupSync = true)

    runStartupActivity()
    assertEquals(1, syncService.scopes.size)
  }

  private fun initialize(initialization: ProjectInitialization, runStartupSync: Boolean = false) {
    val root = projectDir.get()
    when (initialization) {
      ProjectInitialization.PATH -> initializeBazelProject(project, root, runStartupSync)
      ProjectInitialization.STRING -> initializeBazelProject(project, root.toString(), runStartupSync)
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

  enum class ProjectInitialization { PATH, STRING, PROJECT_VIEW }
}

private class RecordingSyncService : ProjectSyncService {
  override val lastSyncTaskId: TaskId? = null
  val scopes = mutableListOf<ProjectSyncScope>()

  override suspend fun sync(scope: ProjectSyncScope) {
    scopes.add(scope)
  }
}
