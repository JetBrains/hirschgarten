package org.jetbrains.bazel.server.tasks

import com.intellij.internal.statistic.FUCollectorTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.buildServer
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bsp.protocol.CompileResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@BazelTestApplication
class BuildTargetTaskFusTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `build target task logs start and finish events`() {
    testBazel.buildServer(BuildServerMock(compileResult = CompileResult(BazelStatus.SUCCESS)))

    val events = FUCollectorTestCase.collectLogEvents(testBazel.disposable) {
      runBlocking {
        runBuildTargetTask(listOf(Label.parse("//app:lib"), Label.parse("//app:test")), testBazel.project)
      }
    }

    val started = assertNotNull(events.singleOrNull { it.group.id == "bazel.build.invocations" && it.event.id == "started" })
    val finished = assertNotNull(events.singleOrNull { it.group.id == "bazel.build.invocations" && it.event.id == "finished" })

    assertEquals(setOf("project", "target_count"), started.event.data.keys)
    assertEquals(2, started.event.data["target_count"])
    assertEquals(setOf("project"), finished.event.data.keys)
  }
}
