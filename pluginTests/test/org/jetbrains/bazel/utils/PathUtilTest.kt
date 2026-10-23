package org.jetbrains.bazel.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.sequences.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.symlinks.createSymlinkOrJunction
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

@DisplayName("uri.toAbsolutePath() tests")
class PathUtilTest {
  @Test
  fun `should return sequence for empty path`() {
    // given
    val emptyPath = Path("")

    // when
    val sequence = emptyPath.allAncestorsSequence()

    // then
    sequence shouldContainExactly sequenceOf(Path(""))
  }

  @Test
  fun `should return sequence with one element for single dir path`() {
    // given
    val path = Path("/")

    // when
    val sequence = path.allAncestorsSequence()

    // then
    sequence shouldContainExactly sequenceOf(Path("/"))
  }

  @Test
  fun `should return sequence for path`() {
    // given
    val path = Path("/path/to/dir/")

    // when
    val sequence = path.allAncestorsSequence()

    // then
    sequence shouldContainExactly sequenceOf(Path("/path/to/dir/"), Path("/path/to/"), Path("/path/"), Path("/"))
  }

  @Test
  fun `calculateCommonAncestor should work correctly`() {
    // given
    val file1 = Path("/path/to/common/dir/some/other/file")
    val file2 = Path("/path/to/common/dir/another/file")

    // when
    val commonAncestor = calculateCommonAncestor(file1, file2)

    // then
    commonAncestor shouldBe Path("/path/to/common/dir")
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
    val file = Path("/path/to/file")

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
        Path("/path/to/file"),
        Path("/path/to/file/inner"),
        Path("/path/to/another/file"),
      )

    // when
    val commonAncestor = files.commonAncestor()

    // then
    commonAncestor shouldBe Path("/path/to")
  }

  @Test
  fun `rebaseOrNull should move a path under from to to`() {
    Path("/a/b/c").rebaseOrNull(from = Path("/a"), to = Path("/x")) shouldBe Path("/x/b/c")
  }

  @Test
  fun `rebaseOrNull should return to for from itself`() {
    Path("/a/b").rebaseOrNull(from = Path("/a/b"), to = Path("/x")) shouldBe Path("/x")
  }

  @Test
  fun `rebaseOrNull should return null for a path not under from`() {
    Path("/a/bc/d").rebaseOrNull(from = Path("/a/b"), to = Path("/x")) shouldBe null
    Path("/other/d").rebaseOrNull(from = Path("/a/b"), to = Path("/x")) shouldBe null
  }

  @Test
  fun `rebaseOrNull should return the path itself when from and to are equal`() {
    Path("/a/b/c").rebaseOrNull(from = Path("/a"), to = Path("/a")) shouldBe Path("/a/b/c")
  }

  @Test
  fun `rebase should move a path under from to to`() {
    Path("/a/b/c").rebase(from = Path("/a"), to = Path("/x")) shouldBe Path("/x/b/c")
  }

  @Test
  fun `rebase should fail for a path not under from`() {
    shouldThrow<IllegalArgumentException> {
      Path("/other/d").rebase(from = Path("/a"), to = Path("/x"))
    }
  }

  @Test
  fun `toRealPathOrNull and toRealPathOrSelf should resolve a symlink`(@TempDir tempDir: Path) {
    val realDir = tempDir.resolve("real").createDirectories().toRealPath()
    val link = tempDir.resolve("link").createSymlinkOrJunction(realDir)

    link.toRealPathOrNull() shouldBe realDir
    link.toRealPathOrSelf() shouldBe realDir
  }

  @Test
  fun `toRealPathOrNull and toRealPathOrSelf should handle a missing file`(@TempDir tempDir: Path) {
    val missing = tempDir.resolve("missing")

    missing.toRealPathOrNull() shouldBe null
    missing.toRealPathOrSelf() shouldBe missing
  }

  @Test
  fun `filterPathsThatDontContainEachOther should work correctly`() {
    // given
    val paths =
      setOf(
        Path("/path/to/file"),
        Path("/path/to/file/inner"),
        Path("/another/path/to/file"),
        Path("/another/path/to/file2"),
        Path("/another/path"),
      )

    // when
    val pathsThatDontContainEachOther = paths.filterPathsThatDontContainEachOther()

    // then
    pathsThatDontContainEachOther shouldBe
      listOf(
        Path("/path/to/file"),
        Path("/another/path"),
      )
  }
}
