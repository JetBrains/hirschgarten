package org.jetbrains.bazel.utils

import io.kotest.matchers.sequences.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

@DisplayName("uri.toAbsolutePath() tests")
class PathUtilTest {
  @Test
  fun `should return sequence for empty path`() {
    // given
    val emptyPath = Paths.get("")

    // when
    val sequence = emptyPath.allAncestorsSequence()

    // then
    sequence shouldContainExactly sequenceOf(Paths.get(""))
  }

  @Test
  fun `should return sequence with one element for single dir path`() {
    // given
    val path = Paths.get("/")

    // when
    val sequence = path.allAncestorsSequence()

    // then
    sequence shouldContainExactly sequenceOf(Paths.get("/"))
  }

  @Test
  fun `should return sequence for path`() {
    // given
    val path = Paths.get("/path/to/dir/")

    // when
    val sequence = path.allAncestorsSequence()

    // then
    sequence shouldContainExactly sequenceOf(Paths.get("/path/to/dir/"), Paths.get("/path/to/"), Paths.get("/path/"), Paths.get("/"))
  }

  @Test
  fun `calculateCommonAncestor should work correctly`() {
    // given
    val file1 = Paths.get("/path/to/common/dir/some/other/file")
    val file2 = Paths.get("/path/to/common/dir/another/file")

    // when
    val commonAncestor = calculateCommonAncestor(file1, file2)

    // then
    commonAncestor shouldBe Paths.get("/path/to/common/dir")
  }

  @Test
  fun `commonAncestor should return null for empty list`() {
    // when
    val commonAncestor = emptyList<Path>().commonAncestor()

    // then
    commonAncestor shouldBe null
  }

  @Test
  fun `commonAncestor should return itself for one path`() {
    // given
    val file = Paths.get("/path/to/file")

    // when
    val commonAncestor = listOf(file).commonAncestor()

    // then
    commonAncestor shouldBe file
  }

  @Test
  fun `commonAncestor should work for three files`() {
    // given
    val files =
      setOf(
        Paths.get("/path/to/file"),
        Paths.get("/path/to/file/inner"),
        Paths.get("/path/to/another/file"),
      )

    // when
    val commonAncestor = files.commonAncestor()

    // then
    commonAncestor shouldBe Paths.get("/path/to")
  }

  @Test
  fun `filterPathsThatDontContainEachOther should work correctly`() {
    // given
    val paths =
      setOf(
        Paths.get("/path/to/file"),
        Paths.get("/path/to/file/inner"),
        Paths.get("/another/path/to/file"),
        Paths.get("/another/path/to/file2"),
        Paths.get("/another/path"),
      )

    // when
    val pathsThatDontContainEachOther = paths.filterPathsThatDontContainEachOther()

    // then
    pathsThatDontContainEachOther shouldBe
      listOf(
        Paths.get("/path/to/file"),
        Paths.get("/another/path"),
      )
  }
}
