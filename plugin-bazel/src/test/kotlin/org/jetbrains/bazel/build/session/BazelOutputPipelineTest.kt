package org.jetbrains.bazel.build.session

import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import com.intellij.build.events.BuildEvents
import com.intellij.testFramework.junit5.impl.TestApplicationExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
class BazelOutputPipelineTest {

  private class RecordingBuildListener : BuildProgressListener {
    data class Record(val buildId: Any, val event: BuildEvent)
    val records = mutableListOf<Record>()
    override fun onEvent(buildId: Any, event: BuildEvent) {
      records += Record(buildId, event)
    }
  }

  private fun newSession(): Pair<BazelBuildSession, RecordingBuildListener> {
    val listener = RecordingBuildListener()
    val descriptor = DefaultBuildDescriptor(Any(), "Bazel build //:lib", "", System.currentTimeMillis())
    val session = BazelBuildSession(listener, descriptor)
    return session to listener
  }

  private fun msgEvent(parentId: Any, text: String): BuildEvent =
    com.intellij.build.events.impl.MessageEventImpl(parentId, MessageEvent.Kind.INFO, null, text, text)

  @Test
  fun `first matching parser wins`() {
    val (session, manager) = newSession()
    session.start()

    val p1 = BuildOutputParser { _, reader, consumer ->
      consumer.accept(msgEvent(reader.parentEventId, "P1"))
      true
    }
    val p2 = BuildOutputParser { _, reader, consumer ->
      consumer.accept(msgEvent(reader.parentEventId, "P2"))
      true
    }

    val pipeline = BazelOutputPipeline(session, listOf(p1, p2))
    pipeline.onLine("anything")

    val msgs = manager.records.map { it.event }.filterIsInstance<MessageEvent>()
    assertEquals(1, msgs.size)
    assertEquals("P1", msgs.single().message)
  }

  @Test
  fun `unparsed line is ignored by pipeline (console shows it)`() {
    val (session, manager) = newSession()
    session.start()

    val pipeline = BazelOutputPipeline(session, emptyList())
    pipeline.onLine("plain line")

    // Pipeline no longer emits OutputBuildEvent for unmatched lines to avoid duplication with console
    val outputs = manager.records.map { it.event }.filter { it is com.intellij.build.events.OutputBuildEvent }
    assertTrue(outputs.isEmpty())
  }

  @Test
  fun `parser exception does not break the stream`() {
    val (session, manager) = newSession()
    session.start()

    val broken = BuildOutputParser { _, _, _ -> throw RuntimeException("boom") }
    val ok = BuildOutputParser { _, reader, consumer ->
      consumer.accept(msgEvent(reader.parentEventId, "OK"))
      true
    }

    val pipeline = BazelOutputPipeline(session, listOf(broken, ok))
    pipeline.onLine("line1") // echoed due to exception
    pipeline.onLine("line2") // handled by ok parser

    val msgs = manager.records.map { it.event }.filterIsInstance<MessageEvent>()
    assertTrue(msgs.any { it.message == "OK" })
  }
}
