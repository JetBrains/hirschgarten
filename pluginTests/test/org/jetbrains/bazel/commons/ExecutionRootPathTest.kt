package org.jetbrains.bazel.commons

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.ExecutionRootPath.Companion.createAncestorRelativePath
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import java.nio.file.Files
import java.nio.file.Path

class ExecutionRootPathTest {

  companion object {
    private fun createMockDirectory(path: String): Path {
      val org = Path.of(path)
      val spy: Path = Mockito.spy(org)
      Mockito.`when`(Files.isDirectory(spy)).then(
        Answer { _: InvocationOnMock? -> true },
      )
      return spy
    }
  }

  @Test
  fun testSingleLevelPathEndInSlash() {
    val executionRootPath = ExecutionRootPath("foo")
    executionRootPath.absoluteOrRelativePath shouldBe Path.of("foo/")

    val executionRootPath2 = ExecutionRootPath("foo/")
    executionRootPath2.absoluteOrRelativePath shouldBe Path.of("foo/")
  }

  @Test
  fun testMultiLevelPathEndInSlash() {
    val executionRootPath = ExecutionRootPath("foo/bar")
    executionRootPath.absoluteOrRelativePath shouldBe Path.of("foo/bar/")

    val executionRootPath2 = ExecutionRootPath("foo/bar/")
    executionRootPath2.absoluteOrRelativePath shouldBe Path.of("foo/bar/")
  }

  @Test
  fun testAbsoluteFileDoesNotGetRerooted() {
    val executionRootPath = ExecutionRootPath("/root/foo/bar")
    val rootedFile = executionRootPath.getPathRootedAt(Path.of("/core/dev"))
    rootedFile shouldBe Path.of("/root/foo/bar")
  }

  @Test
  fun testRelativeFileGetsRerooted() {
    val executionRootPath = ExecutionRootPath("foo/bar")
    val rootedFile = executionRootPath.getPathRootedAt(Path.of("/root"))
    rootedFile shouldBe Path.of("/root/foo/bar")
  }

  @Test
  fun testCreateRelativePathWithTwoRelativePaths() {
    val relativePathFragment: ExecutionRootPath? =
      createAncestorRelativePath(
        createMockDirectory("code/lib/fastmath"),
        createMockDirectory("code/lib/fastmath/lib1"),
      )
    relativePathFragment.shouldNotBeNull()
    relativePathFragment.absoluteOrRelativePath shouldBe Path.of("lib1")
  }

  @Test
  fun testCreateRelativePathWithTwoRelativePathsWithNoRelativePath() {
    val relativePathFragment: ExecutionRootPath? =
      createAncestorRelativePath(
        createMockDirectory("obj/lib/fastmath"), createMockDirectory("code/lib/slowmath"),
      )
    relativePathFragment.shouldBeNull()
  }

  @Test
  fun testCreateRelativePathWithTwoAbsolutePaths() {
    val relativePathFragment: ExecutionRootPath? =
      createAncestorRelativePath(
        createMockDirectory("/code/lib/fastmath"),
        createMockDirectory("/code/lib/fastmath/lib1"),
      )
    relativePathFragment.shouldNotBeNull()
    relativePathFragment.absoluteOrRelativePath shouldBe Path.of("lib1")
  }

  @Test
  fun testCreateRelativePathWithTwoAbsolutePathsWithNoRelativePath() {
    val relativePathFragment: ExecutionRootPath? =
      createAncestorRelativePath(
        createMockDirectory("/obj/lib/fastmath"), createMockDirectory("/code/lib/slowmath"),
      )
    relativePathFragment.shouldBeNull()
  }

  @Test
  fun testCreateRelativePathWithOneAbsolutePathAndOneRelativePathReturnsNull1() {
    val relativePathFragment: ExecutionRootPath? =
      createAncestorRelativePath(
        createMockDirectory("/code/lib/fastmath"),
        createMockDirectory("code/lib/fastmath/lib1"),
      )
    relativePathFragment.shouldBeNull()
  }

  @Test
  fun testCreateRelativePathWithOneAbsolutePathAndOneRelativePathReturnsNull2() {
    val relativePathFragment: ExecutionRootPath? =
      createAncestorRelativePath(
        createMockDirectory("code/lib/fastmath"), createMockDirectory("/code/lib/slowmath"),
      )
    relativePathFragment.shouldBeNull()
  }
}
