package org.jetbrains.bazel.build.session

import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.*
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.testFramework.junit5.impl.TestApplicationExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
class BazelBuildSessionTest {

  private class RecordingBuildListener : BuildProgressListener {
    data class Record(val buildId: Any, val event: BuildEvent)
    val records = mutableListOf<Record>()
    override fun onEvent(buildId: Any, event: BuildEvent) {
      records += Record(buildId, event)
    }
  }

  private fun newSession(title: String = "Bazel build //:lib"): Pair<BazelBuildSession, RecordingBuildListener> {
    val listener = RecordingBuildListener()
    val descriptor = DefaultBuildDescriptor(Any(), title, "", System.currentTimeMillis())
    val session = BazelBuildSession(listener, descriptor)
    return session to listener
  }

  @Test
  fun `start and finish success when no errors and exit code 0`() {
    val (session, manager) = newSession()

    session.start()
    session.finish(exitCode = 0)

    assertTrue(manager.records.any { it.event is StartBuildEvent })
    val finish = manager.records.last().event as FinishBuildEvent
    assertTrue(finish.result is SuccessResultImpl)
  }

  @Test
  fun `finish failure when error message accepted`() {
    val (session, manager) = newSession()

    session.start()
    // Feed an error message
    val err = com.intellij.build.events.impl.MessageEventImpl(
      session.id(),
      MessageEvent.Kind.ERROR,
      null,
      "boom",
      "boom"
    )
    session.accept(err)

    session.finish(exitCode = 0)

    val finish = manager.records.last().event as FinishBuildEvent
    assertTrue(finish.result is FailureResultImpl)
  }

  @Test
  fun `target nodes start and finish`() {
    val (session, manager) = newSession()

    session.start()
    session.onTargetConfigured("//app:lib")
    session.onTargetCompleted("//app:lib", success = true)

    assertTrue(manager.records.any { (it.event as? StartEvent)?.message == "//app:lib" })
    assertTrue(manager.records.any { (it.event as? FinishEvent)?.message == "//app:lib" })
  }
}
