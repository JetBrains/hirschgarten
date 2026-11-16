package org.jetbrains.bazel.clion

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo.CompilationMode
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoCheck
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoPromptService
import org.jetbrains.bazel.clion.run.debug.buildCcDebugInfoRequest
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoChoice
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoRequest
import org.jetbrains.bazel.label.Label
import org.junit.jupiter.api.Test

private val MAIN = Label.parse("@@//main:main")
private val LIB = Label.parse("@@//lib:lib")

@TestApplication
class CcDebugInfoPromptTest {

  @Test
  fun testRequestCountsTargets() {
    val result = result(MAIN to CompilationMode.FASTBUILD, MAIN to CompilationMode.FASTBUILD, LIB to CompilationMode.OPT)
    val request = buildCcDebugInfoRequest("//main:main", result, ".bazelproject", canUseDbg = true, Channel())

    assertThat(request.targetName).isEqualTo("//main:main")
    assertThat(request.targetCount).isEqualTo(2)
    assertThat(request.targetsWithoutDbgCount).isEqualTo(2)
    assertThat(request.details).hasSize(3)
    assertThat(request.expandDetails).isFalse()
    assertThat(request.projectViewFileName).isEqualTo(".bazelproject")
    assertThat(request.canUseDbg).isTrue()
  }

  @Test
  fun testRequestWithMixedCompilationModes() {
    // a transition can set the compilation mode of one target
    val result = result(MAIN to CompilationMode.FASTBUILD, LIB to CompilationMode.DBG)
    val request = buildCcDebugInfoRequest("//main:main", result, "", canUseDbg = false, Channel())

    assertThat(request.targetCount).isEqualTo(2)
    assertThat(request.targetsWithoutDbgCount).isEqualTo(1)
    assertThat(request.expandDetails).isTrue()
  }

  @Test
  fun testAskWithoutFrontend(): Unit = timeoutRunBlocking {
    val service = CcDebugInfoPromptService()

    // the service waits for a frontend until the timeout ends
    val choice = service.ask { request(it) }

    assertThat(choice).isEqualTo(CcDebugInfoChoice.DEBUG_ANYWAY)
  }

  @Test
  fun testAskReturnsTheAnswer(): Unit = timeoutRunBlocking {
    val service = CcDebugInfoPromptService()
    val shown = mutableListOf<CcDebugInfoRequest>()

    val frontend = launch(start = CoroutineStart.UNDISPATCHED) {
      service.requests.collect {
        shown += it
        it.answer.send(CcDebugInfoChoice.USE_DBG)
      }
    }

    val choice = service.ask { request(it) }
    frontend.cancelAndJoin()

    assertThat(choice).isEqualTo(CcDebugInfoChoice.USE_DBG)
    assertThat(shown).hasSize(1)
  }

  @Test
  fun testAskCancelsIfTheAnswerCloses(): Unit = timeoutRunBlocking {
    val service = CcDebugInfoPromptService()

    // for example, the frontend disconnects
    val frontend = launch(start = CoroutineStart.UNDISPATCHED) {
      service.requests.collect { it.answer.close() }
    }

    val choice = service.ask { request(it) }
    frontend.cancelAndJoin()

    assertThat(choice).isEqualTo(CcDebugInfoChoice.CANCEL)
  }
}

private fun result(vararg issues: Pair<Label, CompilationMode>): CcDebugInfoCheck.Result {
  return CcDebugInfoCheck.Result(issues.map { [label, mode] -> CcDebugInfoCheck.Issue(label, ACTION, mode) })
}

private val ACTION: CcDebugInfo.Action = CcDebugInfo.Action.newBuilder()
  .setMnemonic("CppCompile")
  .addArguments("/usr/bin/cc")
  .addArguments("-O2")
  .build()

private fun request(answer: SendChannel<CcDebugInfoChoice>): CcDebugInfoRequest {
  return buildCcDebugInfoRequest("//main:main", result(MAIN to CompilationMode.FASTBUILD), "", canUseDbg = true, answer)
}
