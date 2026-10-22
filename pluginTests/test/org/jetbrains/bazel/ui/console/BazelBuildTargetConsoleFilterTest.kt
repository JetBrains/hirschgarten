package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.application.runReadActionBlocking
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.bazelInfo
import org.jetbrains.bazel.test.framework.testproject.externalRepositoryPath
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_LINE_PREFIX = "There are 39 characters in this string "
private const val TEST_LINE_SUFFIX = " bliep bliep bliep\n"

@BazelTestApplication
class BazelBuildTargetConsoleFilterTest {
  private val myFixture by bazelCodeInsightFixture()
  private val testBazel get() = myFixture.testBazel

  private val project get() = myFixture.project

  private lateinit var filter: BazelBuildTargetConsoleFilter

  @BeforeEach
  fun setUp() {
    filter = BazelBuildTargetConsoleFilter(project)
  }

  @Test
  fun `should match an absolute bazel target without @`() {
    // given
    testBazel.writeFile("plugin-bazel/src/BUILD", testBazelFileContent)
    val bazelTarget = "//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bazel"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an absolute bazel target with a @`() {
    // given
    testBazel.writeFile("plugin-bazel/src/BUILD", testBazelFileContent)
    val bazelTarget = "@//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bazel"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an absolute bazel target with @@`() {
    // given
    testBazel.writeFile("plugin-bazel/src/BUILD", testBazelFileContent)
    val bazelTarget = "@@//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain "plugin-bazel"
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an external target with @@`() {
    // given
    testBazel.sync { repositories { external("externalRepo") } }
    val externalRepositoryRoot = testBazel.bazelInfo.externalRepositoryPath("externalRepo")
    val path = testBazel.writeFile(externalRepositoryRoot.resolve("plugin-bazel/src/BUILD"), testBazelFileContent).toNioPath()
    val bazelTarget = "@@externalRepo//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain path.toString()
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an external target with @`() {
    // given
    testBazel.sync { repositories { external("externalRepo2+", apparentName = "externalRepo2") } }
    val externalRepositoryRoot = testBazel.bazelInfo.externalRepositoryPath("externalRepo2+")
    val path = testBazel.writeFile(externalRepositoryRoot.resolve("plugin-bazel/src/BUILD"), testBazelFileContent).toNioPath()
    val bazelTarget = "@externalRepo2//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 1
    (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo).virtualFile?.toNioPath().toString() shouldContain path.toString()
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should not match an external target which does not exist `() {
    // given
    testBazel.writeFile("plugin-bazel/src/BUILD", testBazelFileContent)
    val bazelTarget = "@@externalRepo3//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should not match non-existing folder`() {
    // given
    testBazel.writeFile("plugin-bazel/src/BUILD", testBazelFileContent)
    val bazelTarget = "@@external//plugin-bazel/test:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 0
  }

  @Test
  fun `should match top level build files`() {
    // given
    testBazel.writeFile("BUILD", testBazelFileContent)
    val bazelTarget = "//:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 1
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should match an absolute bazel target without @ to BUILD_bazel`() {
    // given
    testBazel.writeFile("plugin-bazel/src/BUILD.bazel", testBazelFileContent)
    val bazelTarget = "//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

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
    testBazel.writeFile("BUILD.bazel", testBazelFileContent)
    testBazel.writeFile("BUILD", testBazelFileContent)
    val bazelTarget = "//:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

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
    testBazel.writeFile("plugin-bazel/src/BUILD", testBazelFileContent)
    val bazelTarget = "//plugin-bazel/src:test_fixtures"
    val line = "$TEST_LINE_PREFIX$bazelTarget$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 1
    val hyperLink = (result.resultItems[0].hyperlinkInfo as OpenFileHyperlinkInfo)
    hyperLink.virtualFile?.toNioPath().toString() shouldContain "plugin-bazel"
    runReadActionBlocking { hyperLink.descriptor?.offset } shouldBe 25
    result.resultItems[0].highlightStartOffset shouldBe 100 + TEST_LINE_PREFIX.length
    result.resultItems[0].highlightEndOffset shouldBe 100 + TEST_LINE_PREFIX.length + bazelTarget.length
  }

  @Test
  fun `should not match an file URI followed by colon`() {
    // given
    val path = testBazel.writeFile("plugin-bazel/src/BUILD.bazel", testBazelFileContent).toNioPath()
    val uri = path.toAbsolutePath().toUri()
    val line = "$TEST_LINE_PREFIX$uri:1$TEST_LINE_SUFFIX"

    // when
    val result = runReadActionBlocking { filter.applyFilter(line, line.length + 100) }

    // then
    result!!.resultItems.size shouldBe 0
  }
}
