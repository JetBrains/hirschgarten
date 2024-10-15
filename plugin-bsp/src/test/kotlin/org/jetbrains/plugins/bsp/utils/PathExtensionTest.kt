package org.jetbrains.plugins.bsp.utils

import io.kotest.matchers.sequences.shouldContainExactly

@org.junit.jupiter.api.DisplayName("uri.toAbsolutePath() tests")
class PathExtensionTest {
  @org.junit.jupiter.api.Test
  fun `should return sequence for empty path`() {
    // given
    val emptyPath = java.nio.file.Paths.get("")

    // when
    val sequence = emptyPath.allSubdirectoriesSequence()

    // then
    sequence shouldContainExactly kotlin.sequences.sequenceOf(java.nio.file.Paths.get(""))
  }

  @org.junit.jupiter.api.Test
  fun `should return sequence with one element for single dir path`() {
    // given
    val path = java.nio.file.Paths.get("/")

    // when
    val sequence = path.allSubdirectoriesSequence()

    // then
    sequence shouldContainExactly kotlin.sequences.sequenceOf(java.nio.file.Paths.get("/"))
  }

  @org.junit.jupiter.api.Test
  fun `should return sequence for path`() {
    // given
    val path = java.nio.file.Paths.get("/path/to/dir/")

    // when
    val sequence = path.allSubdirectoriesSequence()

    // then
    sequence shouldContainExactly
        kotlin.sequences.sequenceOf(
          java.nio.file.Paths.get("/path/to/dir/"),
          java.nio.file.Paths.get("/path/to/"),
          java.nio.file.Paths.get("/path/"),
          java.nio.file.Paths.get("/"),
        )
  }
}
