package org.jetbrains.bazel.build.output

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.ArrayDeque

class BazelInstantReaderTest {

  @Test
  fun `readLine and consumedBeyondHead`() {
    val dq = ArrayDeque(listOf("H", "l2", "l3"))
    val reader = BazelInstantReader(dq) { "P" }

    assertEquals("l2", reader.readLine())
    assertEquals(1, reader.consumedBeyondHead)

    assertEquals("l3", reader.readLine())
    assertEquals(2, reader.consumedBeyondHead)

    assertNull(reader.readLine())
    assertEquals(2, reader.consumedBeyondHead)
  }

  @Test
  fun `pushBack works`() {
    val dq = ArrayDeque(listOf("H", "l2"))
    val reader = BazelInstantReader(dq) { "P" }

    assertEquals("l2", reader.readLine())
    reader.pushBack()
    assertEquals(0, reader.consumedBeyondHead)
    assertEquals("l2", reader.readLine())
  }

  @Test
  fun `parent id provider is consulted`() {
    var parent = "A"
    val dq = ArrayDeque(listOf("H"))
    val reader = BazelInstantReader(dq) { parent }

    assertEquals("A", reader.parentEventId)
    parent = "B"
    assertEquals("B", reader.parentEventId)
  }
}
