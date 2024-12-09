package org.jetbrains.bazel.settings

import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import io.kotest.matchers.collections.shouldContain
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

class BazelConsoleFilterTest : WorkspaceModelBaseTest() {
  private lateinit var filter: BazelConsoleFilter

  @BeforeEach
  override fun beforeEach() {
    project.rootDir = projectBasePath.toVirtualFileUrl(virtualFileUrlManager).virtualFile!!
    filter = BazelConsoleFilter(project)
  }

  @Test
  fun `should match an absolute bazel target without @`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget bliep bliep bliep\n"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bsp"
    result.resultItems[0].highlightStartOffset shouldBe 139
    result.resultItems[0].highlightEndOffset shouldBe 169
  }

  @Test
  fun `should match an absolute bazel target with a @`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "@//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget bliep bliep bliep\n"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bsp"
    result.resultItems[0].highlightStartOffset shouldBe 139
    result.resultItems[0].highlightEndOffset shouldBe 170
  }

  @Test
  fun `should match an absolute bazel target with @@`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "@@//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget bliep bliep bliep\n"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bsp"
    result.resultItems[0].highlightStartOffset shouldBe 139
    result.resultItems[0].highlightEndOffset shouldBe 171
  }

  @Test
  fun `should not match an external target with @`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "@external//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget bliep bliep bliep\n"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should not match an external target with @@`() {
    createBazelFileInProject("plugin-bsp/src")
    val bazelTarget = "@@external//plugin-bsp/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget bliep bliep bliep\n"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should match top level build files`() {
    createBazelFileInProject(".")
    val bazelTarget = "//:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget bliep bliep bliep\n"
    val result = filter.applyFilter(line, line.length + 100)
    result!!.resultItems.size shouldBe 1
    result.resultItems[0].highlightStartOffset shouldBe 139
    result.resultItems[0].highlightEndOffset shouldBe 155
  }

  private fun createBazelFileInProject(relativePath: String): Path {
    projectBasePath
      .resolve(relativePath)
      .resolve("BUILD")
      .createDirectories()
      .createFile()
      .also { LocalFileSystem.getInstance().refreshAndFindFileByNioFile(it) }
  }
}
