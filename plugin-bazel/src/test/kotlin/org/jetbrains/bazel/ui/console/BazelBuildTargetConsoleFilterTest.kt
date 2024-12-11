package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

private const val TEST_LINE_PREFIX = "There are 39 characters in this string "
private const val TEST_LINE_SUFFIX = " bliep bliep bliep\n"

class BazelBuildTargetConsoleFilterTest : WorkspaceModelBaseTest() {
  private lateinit var filter: BazelBuildTargetConsoleFilter

  @BeforeEach
  override fun beforeEach() {
    project.rootDir = projectBasePath.toVirtualFileUrl(virtualFileUrlManager).virtualFile!!
    filter = BazelBuildTargetConsoleFilter(project)
  }

  @Test
  fun `should match an absolute bazel target without @`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bsp"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an absolute bazel target with a @`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "@//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bsp"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an absolute bazel target with @@`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "@@//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bsp"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should not match an external target with @`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "@external//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should not match an external target with @@`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "@@external//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should not match non-existing folder`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "@@external//plugin-bsp/test:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should match top level build files`() {
    createBazelFileInProject(".")
    val bazelTarget = "//:format"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an absolute bazel target without @ to BUILD_bazel`() {
    createBazelFileInProject("plugin-bsp/src", "BUILD.bazel")
    val bazelTarget = "//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bsp"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile.toString().endsWith("BUILD.bazel") shouldBe true
  }

  @Test
  fun `BUILD_bazel has higher priority than BUILD`() {
    createBazelFileInProject(".", "BUILD.bazel")
    createBazelFileInProject(".", "BUILD")

    val bazelTarget = "//:format"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile.toString().endsWith("BUILD.bazel") shouldBe true
  }

  private fun createBazelFileInProject(relativePath: String, buildFileName: String = "BUILD"): Path =
    projectBasePath
      .resolve(relativePath)
      .createDirectories()
      .resolve(buildFileName)
      .createFile()
      .also { LocalFileSystem.getInstance().refreshAndFindFileByNioFile(it) }
}
