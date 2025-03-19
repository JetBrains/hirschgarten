package org.jetbrains.bazel.java.sync

import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.sync.BaseTargetInfos
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresProvider
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JavaSyncHookTest : MockProjectBaseTest() {
  lateinit var hook: ProjectSyncHook
  lateinit var virtualFileUrlManager: VirtualFileUrlManager

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    // givens
    hook = JavaSyncHook()
    virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
  }

  @Test
  fun `should add no modules and no libraries for no targets no libraries`() {
    // given
    val server = BuildServerMock(
      workspaceLibrariesResult = WorkspaceLibrariesResult(emptyList()),
      workspaceNonModuleTargetsResult = NonModuleTargetsResult(emptyList()),
    )
    val diff = AllProjectStructuresProvider(project).newDiff()

    val emptyBaseInfos = BaseTargetInfos(emptyList(), emptyList())

    // when
    runBlocking {
      reportSequentialProgress { reporter ->
        val environment =
          ProjectSyncHook.ProjectSyncHookEnvironment(
            project = project,
            syncScope = SecondPhaseSync,
            server = server,
            diff = diff,
            taskId = "test",
            progressReporter = reporter,
            baseTargetInfos = emptyBaseInfos,
          )
        hook.onSync(environment)
      }
    }

    // then
    diff.workspaceModelDiff.mutableEntityStorage.entities(ModuleEntity::class.java).toList() shouldBe emptyList()
    diff.workspaceModelDiff.mutableEntityStorage.entities(LibraryEntity::class.java).toList() shouldBe emptyList()
  }

  @Test
  fun `should add module for one target`() {
    // given
    val server = BuildServerMock(
      workspaceLibrariesResult = WorkspaceLibrariesResult(emptyList()),
      workspaceNonModuleTargetsResult = NonModuleTargetsResult(emptyList()),
    )
    val diff = AllProjectStructuresProvider(project).newDiff()

    val emptyBaseInfos = BaseTargetInfos(emptyList(), emptyList())

    // when
    runBlocking {
      reportSequentialProgress { reporter ->
        val environment =
          ProjectSyncHook.ProjectSyncHookEnvironment(
            project = project,
            syncScope = SecondPhaseSync,
            server = server,
            diff = diff,
            taskId = "test",
            progressReporter = reporter,
            baseTargetInfos = emptyBaseInfos,
          )
        hook.onSync(environment)
      }
    }

    // then
    diff.workspaceModelDiff.mutableEntityStorage.entities(ModuleEntity::class.java).toList() shouldBe emptyList()
  }
}
