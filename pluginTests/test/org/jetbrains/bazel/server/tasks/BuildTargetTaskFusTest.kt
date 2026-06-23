package org.jetbrains.bazel.server.tasks

import com.intellij.internal.statistic.FUCollectorTestCase
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockBuildServerService
import org.jetbrains.bsp.protocol.CompileResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestApplication
class BuildTargetTaskFusTest {
  private val projectFixture = projectFixture()
  private val project: Project by projectFixture

  @Test
  fun `build target task logs start and finish events`(@TestDisposable disposable: Disposable) {
    project.replaceService(
      BazelServerService::class.java,
      MockBuildServerService(BuildServerMock(compileResult = CompileResult(BazelStatus.SUCCESS))),
      disposable,
    )

    val events = FUCollectorTestCase.collectLogEvents(disposable) {
      runBlocking {
        runBuildTargetTask(listOf(Label.parse("//app:lib"), Label.parse("//app:test")), project)
      }
    }

    val started = assertNotNull(events.singleOrNull { it.group.id == "bazel.build.invocations" && it.event.id == "started" })
    val finished = assertNotNull(events.singleOrNull { it.group.id == "bazel.build.invocations" && it.event.id == "finished" })

    assertEquals(setOf("project", "target_count"), started.event.data.keys)
    assertEquals(2, started.event.data["target_count"])
    assertEquals(setOf("project"), finished.event.data.keys)
  }
}
