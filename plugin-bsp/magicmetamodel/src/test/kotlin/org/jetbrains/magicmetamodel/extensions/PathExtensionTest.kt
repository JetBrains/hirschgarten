package org.jetbrains.magicmetamodel.extensions

import io.kotest.matchers.sequences.shouldContainExactly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@DisplayName("uri.toAbsolutePath() tests")
class PathExtensionTest {
  @Test
  fun `should return sequence for empty path`() {
    // given
    val emptyPath = Paths.get("")

    // when
    val sequence = emptyPath.allSubdirectoriesSequence()

    // then
    sequence shouldContainExactly sequenceOf(Paths.get(""))
  }

  @Test
  fun `should return sequence with one element for single dir path`() {
    // given
    val path = Paths.get("/")

    // when
    val sequence = path.allSubdirectoriesSequence()

    // then
    sequence shouldContainExactly sequenceOf(Paths.get("/"))
  }

  @Test
  fun `should return sequence for path`() {
    // given
    val path = Paths.get("/path/to/dir/")

    // when
    val sequence = path.allSubdirectoriesSequence()

    // then
    print(sequence.toList())
    sequence shouldContainExactly sequenceOf(
      Paths.get("/path/to/dir/"),
      Paths.get("/path/to/"),
      Paths.get("/path/"),
      Paths.get("/"),
    )
  }
}
