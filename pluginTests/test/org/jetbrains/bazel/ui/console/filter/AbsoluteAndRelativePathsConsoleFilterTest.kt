package org.jetbrains.bazel.ui.console.filter

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.application.readAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString

private const val TEST_LINE_PREFIX = "it really doesn't matter what is here"

class AbsoluteAndRelativePathsConsoleFilterTest : WorkspaceModelBaseTest() {
  private lateinit var filter: AbsoluteAndRelativePathsConsoleFilter

  @BeforeEach
  override fun beforeEach() {
    // given
    project.rootDir = projectBasePath.toVirtualFileUrl(virtualFileUrlManager).virtualFile!!
    filter = AbsoluteAndRelativePathsConsoleFilter(project)
  }

  @Test
  fun `should match an absolute path without coordinates`() {
    // given
    val path = createPathInProject("some/path/in/the/project")

    val line = "$TEST_LINE_PREFIX $path blah blah blah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 1

    val expectedStartOffset = TEST_LINE_PREFIX.length + 1
    result.resultItems[0].shouldHave(
      expectedStartOffset = expectedStartOffset,
      expectedEndOffset = expectedStartOffset + path.pathString.length,
      expectedPath = path.pathString,
    )
  }

  @Test
  fun `should match a relative to project root path without coordinates`() {
    // given
    val relativePath = "some/path/in/the/project"
    val path = createPathInProject(relativePath)

    val line = "$TEST_LINE_PREFIX $relativePath blah blah blah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 1

    val expectedStartOffset = TEST_LINE_PREFIX.length + 1
    result.resultItems[0].shouldHave(
      expectedStartOffset = expectedStartOffset,
      expectedEndOffset = expectedStartOffset + relativePath.length,
      expectedPath = path.pathString,
    )
  }

  @Test
  fun `should match an absolute path with coordinates`() {
    // given
    val path = createPathInProject("some/path/in/the/project")

    val line = "$TEST_LINE_PREFIX $path:12:37: blah blah blah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 1

    val expectedStartOffset = TEST_LINE_PREFIX.length + 1
    val coordinatesLength = ":12:37".length
    result.resultItems[0].shouldHave(
      expectedStartOffset = expectedStartOffset,
      expectedEndOffset = expectedStartOffset + path.pathString.length + coordinatesLength,
      expectedPath = path.pathString,
      expectedLine = 11,
      expectedColumn = 36,
    )
  }

  @Test
  fun `should match a relative to project root path with coordinates`() {
    // given
    val relativePath = "some/path/in/the/project"
    val path = createPathInProject(relativePath)

    val line = "$TEST_LINE_PREFIX $relativePath:12:37: blah blah blah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 1

    val expectedStartOffset = TEST_LINE_PREFIX.length + 1
    val coordinatesLength = ":12:37".length
    result.resultItems[0].shouldHave(
      expectedStartOffset = expectedStartOffset,
      expectedEndOffset = expectedStartOffset + relativePath.length + coordinatesLength,
      expectedPath = path.pathString,
      expectedLine = 11,
      expectedColumn = 36,
    )
  }

  @Test
  fun `should match a relative path in the project root`() {
    // given
    val relativePath = "some/path/in/the/project"
    val path = createPathInProject(relativePath)

    val line = "$TEST_LINE_PREFIX $relativePath:12:37: blah blah blah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 1

    val expectedStartOffset = TEST_LINE_PREFIX.length + 1
    val coordinatesLength = ":12:37".length
    result.resultItems[0].shouldHave(
      expectedStartOffset = expectedStartOffset,
      expectedEndOffset = expectedStartOffset + relativePath.length + coordinatesLength,
      expectedPath = path.pathString,
      expectedLine = 11,
      expectedColumn = 36,
    )
  }

  @Test
  fun `should not match a relative path which does not exist in the project root`() {
    // given
    val path = "this/path/is/not/even/under/the/project/root"

    val line = "$TEST_LINE_PREFIX $path blah blah blah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should not match an absolute path which does not exist at all`() {
    // given
    val path = "/this/absolute/path/does/not/even/exist/at/all"

    val line = "$TEST_LINE_PREFIX $path blah blah blah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should not match a relative path in the project root (no slashes in the path)`() {
    // given
    val relativePath = "a_file_in_the_root_and_it_should_not_be_matched"
    createPathInProject(relativePath)

    val line = "$TEST_LINE_PREFIX $relativePath blah blah blah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should not match a single slash`() {
    // given

    val line = "$TEST_LINE_PREFIX / blah blah blah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should match multiple paths in one line`() {
    // given
    val relativePath1 = "some/path/number/1/in/the/project"
    val path1 = createPathInProject(relativePath1)
    val path2 = createPathInProject("some/path/number/2/in/the/project")

    val line = "$TEST_LINE_PREFIX $path1:12:37: blah blah blah $path2 blaaaaah\n"

    // when
    val result = filter.applyFilter(line, line.length)

    // then
    result!!.resultItems.size shouldBe 2

    val expectedStartOffset1 = TEST_LINE_PREFIX.length + 1
    val coordinatesLength1 = ":12:37".length
    val expectedEndOffset1 = expectedStartOffset1 + path1.pathString.length + coordinatesLength1
    result.resultItems[0].shouldHave(
      expectedStartOffset = expectedStartOffset1,
      expectedEndOffset = expectedEndOffset1,
      expectedPath = path1.pathString,
      expectedLine = 11,
      expectedColumn = 36,
    )

    val expectedStartOffset2 = expectedEndOffset1 + ": blah blah blah ".length
    result.resultItems[1].shouldHave(
      expectedStartOffset = expectedStartOffset2,
      expectedEndOffset = expectedStartOffset2 + path2.pathString.length,
      expectedPath = path2.pathString,
    )
  }

  private fun createPathInProject(relativePath: String): Path =
    projectBasePath
      .resolve(relativePath)
      .createDirectories()
      .also { LocalFileSystem.getInstance().refreshAndFindFileByNioFile(it) }

  private fun Filter.ResultItem.shouldHave(
    expectedStartOffset: Int,
    expectedEndOffset: Int,
    expectedPath: String,
    expectedLine: Int = 0,
    expectedColumn: Int = 0,
  ) {
    highlightStartOffset shouldBe expectedStartOffset
    highlightEndOffset shouldBe expectedEndOffset

    val info = hyperlinkInfo as? OpenFileHyperlinkInfo
    info?.virtualFile?.path shouldBe expectedPath
    runBlocking {
      readAction {
        info?.descriptor?.line shouldBe expectedLine
        info?.descriptor?.column shouldBe expectedColumn
      }
    }
  }
}
