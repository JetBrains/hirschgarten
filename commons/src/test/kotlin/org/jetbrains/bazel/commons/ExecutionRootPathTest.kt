/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.commons

import com.google.idea.testing.BazelTestApplication
import org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import java.nio.file.Path
import kotlin.io.path.isDirectory

/** Tests execution root path.  */
@BazelTestApplication
class ExecutionRootPathTest {
  @Test
  fun testSingleLevelPathEndInSlash() {
    val executionRootPath = ExecutionRootPath("foo")
    assertEquals(executionRootPath.absoluteOrRelativePath, (Path.of("foo/")))

    val executionRootPath2 = ExecutionRootPath("foo/")
    assertEquals(executionRootPath2.absoluteOrRelativePath,Path.of("foo/"))
  }

  @Test
  fun testMultiLevelPathEndInSlash() {
    val executionRootPath = ExecutionRootPath("foo/bar")
    assertEquals(executionRootPath.absoluteOrRelativePath,Path.of("foo/bar/"))

    val executionRootPath2 = ExecutionRootPath("foo/bar/")
    assertEquals(executionRootPath2.absoluteOrRelativePath,Path.of("foo/bar/"))
  }

  @Test
  fun testAbsoluteFileDoesNotGetRerooted() {
    val executionRootPath = ExecutionRootPath("/root/foo/bar")
    val rootedFile = executionRootPath.getPathRootedAt(Path.of("/core/dev"))
    assertEquals(rootedFile,(Path.of("/root/foo/bar")))
  }

  @Test
  fun testRelativeFileGetsRerooted() {
    val executionRootPath = ExecutionRootPath("foo/bar")
    val rootedFile = executionRootPath.getPathRootedAt(Path.of("/root"))
    assertEquals(rootedFile,Path.of("/root/foo/bar"))
  }

  @Test
  fun testCreateRelativePathWithTwoRelativePaths() {
    val relativePathFragment =
      ExecutionRootPath.createAncestorRelativePath(
        createMockDirectory("code/lib/fastmath"),
        createMockDirectory("code/lib/fastmath/lib1"),
      )
    assertNotNull(relativePathFragment)
    assertEquals(relativePathFragment.absoluteOrRelativePath,Path.of("lib1"))
  }

  @Test
  fun testCreateRelativePathWithTwoRelativePathsWithNoRelativePath() {
    val relativePathFragment =
      ExecutionRootPath.createAncestorRelativePath(
        createMockDirectory("obj/lib/fastmath"), createMockDirectory("code/lib/slowmath"),
      )
    assertNull(relativePathFragment)
  }

  @Test
  fun testCreateRelativePathWithTwoAbsolutePaths() {
    val relativePathFragment =
      ExecutionRootPath.createAncestorRelativePath(
        createMockDirectory("/code/lib/fastmath"),
        createMockDirectory("/code/lib/fastmath/lib1"),
      )
    assertNotNull(relativePathFragment)
    assertEquals(relativePathFragment.absoluteOrRelativePath, Path.of("lib1"))
  }

  @Test
  fun testCreateRelativePathWithTwoAbsolutePathsWithNoRelativePath() {
    val relativePathFragment =
      ExecutionRootPath.createAncestorRelativePath(
        createMockDirectory("/obj/lib/fastmath"), createMockDirectory("/code/lib/slowmath"),
      )
    assertNull(relativePathFragment)
  }

  @Test
  fun testCreateRelativePathWithOneAbsolutePathAndOneRelativePathReturnsNull1() {
    val relativePathFragment =
      ExecutionRootPath.createAncestorRelativePath(
        createMockDirectory("/code/lib/fastmath"),
        createMockDirectory("code/lib/fastmath/lib1"),
      )
    assertNull(relativePathFragment)
  }

  @Test
  fun testCreateRelativePathWithOneAbsolutePathAndOneRelativePathReturnsNull2() {
    val relativePathFragment =
      ExecutionRootPath.createAncestorRelativePath(
        createMockDirectory("code/lib/fastmath"), createMockDirectory("/code/lib/slowmath"),
      )
    assertNull(relativePathFragment)
  }

  companion object {
    private fun createMockDirectory(path: String): Path {
      val org = Path.of(path)
      val spy: Path = Mockito.spy(org)
      Mockito.`when`(spy.isDirectory())
        .then(Answer { true })
      return spy
    }
  }
}
