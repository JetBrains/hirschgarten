package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.io.findOrCreateFile
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.config.rootDir
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

private const val TEST_LINE_PREFIX = "There are 39 characters in this string "
private const val TEST_LINE_SUFFIX = " bliep bliep bliep\n"

@RunWith(JUnit4::class)
class BazelBuildTargetConsoleFilterTest : BasePlatformTestCase() {
  private lateinit var filter: BazelBuildTargetConsoleFilter

  override fun setUp() {
    super.setUp()
    val virtualFileManager = VirtualFileManager.getInstance()
    project.rootDir = virtualFileManager.findFileByNioPath(Path(project.basePath!!))!!
    filter = BazelBuildTargetConsoleFilter(project)
  }

  @Test
  fun `should match an absolute bazel target without @`() {
    // given
    createBazelFileInProject("plugin-bazel/src")
    val bazelTarget = "//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bazel"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an absolute bazel target with a @`() {
    // given
    createBazelFileInProject("plugin-bazel/src")
    val bazelTarget = "@//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bazel"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an absolute bazel target with @@`() {
    // given
    createBazelFileInProject("plugin-bazel/src")
    val bazelTarget = "@@//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bazel"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should not match an external target with @`() {
    // given
    createBazelFileInProject("plugin-bazel/src")
    val bazelTarget = "@external//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should not match an external target with @@`() {
    // given
    createBazelFileInProject("plugin-bazel/src")
    val bazelTarget = "@@external//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should not match non-existing folder`() {
    // given
    createBazelFileInProject("plugin-bazel/src")
    val bazelTarget = "@@external//plugin-bazel/test:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should match top level build files`() {
    // given
    createBazelFileInProject(".")
    val bazelTarget = "//:format"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 1
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an absolute bazel target without @ to BUILD_bazel`() {
    // given
    createBazelFileInProject("plugin-bazel/src", "BUILD.bazel")
    val bazelTarget = "//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bazel"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile.toString().endsWith("BUILD.bazel") shouldBe true
  }

  @Test
  fun `BUILD_bazel has higher priority than BUILD`() {
    // given
    createBazelFileInProject(".", "BUILD.bazel")
    createBazelFileInProject(".", "BUILD")
    val bazelTarget = "//:format"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 1
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile.toString().endsWith("BUILD.bazel") shouldBe true
  }

  val testBazelFileContent = """
proto_library(
  name = "test_fixtures",
  srcs = glob(["*.kt"]),
)
  """

  @Test
  fun `can jump to correct line`() {
    // given
    createBazelFileInProject("plugin-bazel/src")
    val bazelTarget = "//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 1
    val hyperLink = (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo)
    hyperLink.virtualFile?.toNioPath().toString() shouldContain "plugin-bazel"
    hyperLink.descriptor?.offset shouldBe 16
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should not match an file URI followed by colon`() {
    // given
    val path = createBazelFileInProject("plugin-bazel/src", "BUILD.bazel")
    val uri = path.toAbsolutePath().toUri()
    val line = "$TEST_LINE_PREFIX$uri:1$TEST_LINE_SUFFIX"

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 0
  }

  private fun createBazelFileInProject(relativePath: String, buildFileName: String = "BUILD"): Path =
    runWriteAction {
      project.rootDir
        .toNioPath()
        .resolve(relativePath)
        .createDirectories()
        .resolve(buildFileName)
        .findOrCreateFile()
        .also {
          it.writeText(testBazelFileContent)
        }.also { LocalFileSystem.getInstance().refreshAndFindFileByNioFile(it) }
    }
}
