package org.jetbrains.bazel.sync.task

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.testFramework.registerOrReplaceServiceInstance
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.workspace.model.test.framework.BazelWorkspaceResolveServiceMock
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockBuildServerService
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("Partial sync tests")
class PartialSyncTest : MockProjectBaseTest() {

  @BeforeEach
  fun beforeAll() {
    project.rootDir = VirtualFileManager.getInstance().findFileByNioPath(Path(project.basePath!!))!!
  }

  @Test
  fun `partial sync should call enabled sync hooks`() {
    // given
    project.registerOrReplaceServiceInstance(BazelServerService::class.java, MockBuildServerService(BuildServerMock()), disposable)
    project.registerOrReplaceServiceInstance(
      BazelWorkspaceResolveService::class.java,
      BazelWorkspaceResolveServiceMock(
        resolvedWorkspace = BazelResolvedWorkspace(targets = listOf()),
        bazelProject = WorkspaceBuildTargetsResult(mapOf(), setOf()),
      ),
      disposable,
    )

    val syncHook = TestProjectSyncHook()
    ProjectSyncHook.ep.registerExtension(syncHook)

    val target = Label.parse("//foo/bar:baz")
    val scope = PartialProjectSync(targetsToSync = listOf(target))

    // when
    runBlocking {
      ProjectSyncTask(project).sync(syncScope = scope, buildProject = false)
    }

    // then
    syncHook.wasCalled shouldBe true
  }

  @Test
  fun `partial sync source filter should match only targeted module entity sources`() {
    val moduleNameA = "foo.bar.baz"
    val moduleNameB = "other.module"
    val targetedModuleNames = setOf(moduleNameA)

    val sourceA = BazelModuleEntitySource(moduleNameA)
    val sourceB = BazelModuleEntitySource(moduleNameB)
    val projectSource = BazelProjectEntitySource

    val filter: (EntitySource) -> Boolean = { entitySource ->
      entitySource is BazelModuleEntitySource && entitySource.moduleName in targetedModuleNames
    }

    filter(sourceA) shouldBe true
    filter(sourceB) shouldBe false
    filter(projectSource) shouldBe false
  }

  @Test
  fun `full sync source filter should match all BazelEntitySource instances`() {
    val sourceA = BazelModuleEntitySource("module.a")
    val sourceB = BazelModuleEntitySource("module.b")
    val projectSource = BazelProjectEntitySource

    val filter: (EntitySource) -> Boolean = { entitySource ->
      entitySource is BazelEntitySource
    }

    filter(sourceA) shouldBe true
    filter(sourceB) shouldBe true
    filter(projectSource) shouldBe true
  }
}
