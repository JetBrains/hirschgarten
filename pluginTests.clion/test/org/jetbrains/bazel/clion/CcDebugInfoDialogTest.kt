package org.jetbrains.bazel.clion

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo.CompilationMode
import com.intellij.clion.testFramework.nolang.junit5.debugger.CidrCloseDebugSessionsExtension
import com.intellij.clion.testFramework.nolang.junit5.debugger.debugProfileFixture
import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGdbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.GdbDebugProfileState
import com.jetbrains.cidr.execution.CidrExecutionFixture
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoChoice
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.EXECUTION_TIMEOUT
import org.jetbrains.bazel.fixtures.ccDebugInfoDialogFixture
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.fixtures.startCcDebugSession
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtendWith

@EnabledOnOs(OS.LINUX)
@CcTestApplication
@ExtendWith(CidrCloseDebugSessionsExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcDebugInfoDialogTest {

  private val projectFixture = clionBazelProjectFixture("clion/debug", buildProject = true, bazelVersion = BazelVersions.BAZEL_9) {
    addBuildFlags("--compilation_mode=fastbuild")
  }

  @Suppress("unused")
  private val debugProfile = debugProfileFixture(projectFixture, CLionGdbDebugProfileType.ID, GdbDebugProfileState())

  private val debugInfoDialogFixture = ccDebugInfoDialogFixture(projectFixture,  choice = CcDebugInfoChoice.DEBUG_ANYWAY)

  private val project by projectFixture
  private val debugInfoDialog by debugInfoDialogFixture

  @Test
  fun testShowsDialogForFastbuild(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    // the fixture answers with debug anyway, thus the session starts without debug info
    val context = project.startCcDebugSession("//main:main")
    assertThat(CidrExecutionFixture.waitFor(context.process.processHandler)).isTrue()

    val requests = debugInfoDialog.takeRequests()
    assertThat(requests).hasSize(1)

    val request = requests.single()
    assertThat(request.canUseDbg).isTrue()
    assertThat(request.result.issues.map { it.compilationMode }).containsOnly(CompilationMode.FASTBUILD)
    assertThat(request.result.issues.map { it.label }).contains(Label.parse("@@//main:main"), Label.parse("@@//lib/default:lib"))
  }
}
