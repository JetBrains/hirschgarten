package org.jetbrains.bazel.clion

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetKey
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.run.debug.findSnapshotTarget
import org.jetbrains.bazel.fixtures.TestEvent
import org.jetbrains.bazel.fixtures.ccProject
import org.jetbrains.bazel.fixtures.withTestDebugContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bsp.protocol.BuildTarget
import org.junit.jupiter.api.Test

private val MAIN = Label.parse("@@//main:main")

private const val HOST = "1111111"
private const val TARGET = "2222222"

@TestApplication
class CcDebugTargetInfoTest {

  private val snapshot = ccProject {
    ccBinary {
      label("//main:main")
      configurationId(HOST)
    }
    ccBinary {
      label("//main:main")
      configurationId(TARGET)
    }
  }

  @Test
  fun testUsesConfigurationOfDebugBuild() {
    for (configuration in listOf(HOST, TARGET)) {
      val [events, target] = find(MAIN, listOf(debugTarget(configuration)))

      assertThat(events).isEmpty()
      assertThat(target?.key?.configuration).isEqualTo(WorkspaceConfigurationId.of(configuration))
    }
  }

  @Test
  fun testFallsBackToOtherConfiguration() {
    val [events, target] = find(MAIN, listOf(debugTarget("3333333")))
    assertThat(target?.key?.label).isEqualTo(MAIN)

    val event = events.single()
    assertThat(event.severity).isEqualTo(MessageEvent.Kind.WARNING)
    assertThat(event.message).isEqualTo("Debugging $MAIN in an unknown configuration")
  }

  @Test
  fun testFallsBackWithoutWarning() {
    val [events, target] = find(MAIN, listOf(debugTarget("3333333")), reportUnknownConfiguration = false)

    assertThat(events).isEmpty()
    assertThat(target?.key?.label).isEqualTo(MAIN)
  }

  @Test
  fun testWithoutDebugTargets() {
    val [events, target] = find(MAIN, null)

    assertThat(events).isEmpty()
    assertThat(target?.key?.label).isEqualTo(MAIN)
  }

  @Test
  fun testUnknownTarget() {
    val [events, target] = find(Label.parse("@@//other:other"), listOf(debugTarget(TARGET)))

    assertThat(events).isEmpty()
    assertThat(target).isNull()
  }

  private fun find(
    label: Label,
    debugTargets: List<TargetIdeInfo>?,
    reportUnknownConfiguration: Boolean = true,
  ): Pair<List<TestEvent>, BuildTarget?> = timeoutRunBlocking {
    // the lookup does not use the project, thus the default project is sufficient
    val project = ProjectManager.getInstance().defaultProject

    val [ctx, result] = withTestDebugContext(project, label) { findSnapshotTarget(snapshot, debugTargets, reportUnknownConfiguration) }
    ctx.events to result
  }
}

private fun debugTarget(configuration: String): TargetIdeInfo {
  return TargetIdeInfo.newBuilder()
    .setKey(TargetKey.newBuilder().setLabel(MAIN.toString()).setConfiguration(configuration))
    .build()
}
