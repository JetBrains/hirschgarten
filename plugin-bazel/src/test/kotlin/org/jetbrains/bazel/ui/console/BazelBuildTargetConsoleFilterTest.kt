package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.io.findOrCreateFile
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.io.delete
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingService
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
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
    BazelRepoMappingService.getInstance(project).canonicalRepoNameToPath = mapOf("" to project.rootDir.toNioPath())
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
  fun `should match an external target with @@`() {
    // given
    val externalRootDir = createTempDirectory("externalRepo")
    val path = createBazelFile(externalRootDir, "plugin-bazel/src")
    val bazelTarget = "@@externalRepo//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"
    BazelRepoMappingService.getInstance(project).canonicalRepoNameToPath =
      mapOf("externalRepo" to externalRootDir, "" to project.rootDir.toNioPath())

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain path.toString()
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length

    externalRootDir.delete(true)
  }

  @Test
  fun `should match an external target with @`() {
    // given
    val externalRootDir = createTempDirectory("externalRepo2")
    val path = createBazelFile(externalRootDir, "plugin-bazel/src")
    val bazelTarget = "@externalRepo2//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    BazelRepoMappingService.getInstance(project).canonicalRepoNameToPath =
      mapOf("externalRepo2+" to externalRootDir, "" to project.rootDir.toNioPath())
    BazelRepoMappingService.getInstance(project).apparentRepoNameToCanonicalName = mapOf("externalRepo2" to "externalRepo2+")

    // when
    val result = filter.applyFilter(line, line.length + 100)

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain path.toString()
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length

    externalRootDir.delete(true)
  }

  @Test
  fun `should not match an external target which does not exist `() {
    // given
    createBazelFileInProject("plugin-bazel/src")
    val bazelTarget = "@@externalRepo3//plugin-bazel/src:test_fixtures"
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
    val bazelTarget = "//:test_fixtures"
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
    val bazelTarget = "//:test_fixtures"
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
    hyperLink.descriptor?.offset shouldBe 25
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

  private fun createBazelFile(
    rootDir: Path,
    relativePath: String,
    buildFileName: String = "BUILD",
  ): Path =
    runWriteAction {
      rootDir
        .resolve(relativePath)
        .createDirectories()
        .resolve(buildFileName)
        .findOrCreateFile()
        .also {
          it.writeText(testBazelFileContent)
        }.also { LocalFileSystem.getInstance().refreshAndFindFileByNioFile(it) }
    }

  private fun createBazelFileInProject(relativePath: String, buildFileName: String = "BUILD"): Path =
    createBazelFile(project.rootDir.toNioPath(), relativePath, buildFileName)
}
