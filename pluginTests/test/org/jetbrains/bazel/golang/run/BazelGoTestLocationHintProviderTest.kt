package org.jetbrains.bazel.golang.run

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.server.bep.MockBuildTaskEventsHandler
import org.jetbrains.bazel.server.bep.TestXmlParser
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.util.BspClientTestNotifier
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TestStart
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

internal class BazelGoTestLocationHintProviderTest : MockProjectBaseTest() {
  @Test
  fun `should provide correct gotest location hints`(@TempDir tempDir: Path) {
    val testXml = """
      <testsuites>
      	<testsuite errors="0" failures="0" skipped="0" tests="1" time="0.000" name="github.com/buildbuddy-io/buildbuddy/cli/arg.TestAdd" timestamp="2026-08-27T12:59:15.019Z">
      		<testcase classname="arg" name="TestAdd" time="0.000"></testcase>
      	</testsuite>
      	<testsuite errors="0" failures="0" skipped="0" tests="5" time="0.000" name="github.com/buildbuddy-io/buildbuddy/cli/arg.TestAddTableDriven" timestamp="2026-08-27T12:59:15.019Z">
      		<testcase classname="arg" name="TestAddTableDriven" time="0.000"></testcase>
      		<testcase classname="arg" name="TestAddTableDriven/mixed" time="0.000"></testcase>
      		<testcase classname="arg" name="TestAddTableDriven/negative" time="0.000"></testcase>
      		<testcase classname="arg" name="TestAddTableDriven/positive" time="0.000"></testcase>
      		<testcase classname="arg" name="TestAddTableDriven/zeros" time="0.000"></testcase>
      	</testsuite>
      	<testsuite errors="0" failures="0" skipped="0" tests="1" time="0.000" name="github.com/buildbuddy-io/buildbuddy/cli/arg.TestIsPrime" timestamp="2026-08-27T12:59:15.019Z">
      		<testcase classname="arg" name="TestIsPrime" time="0.000"></testcase>
      	</testsuite>
      	<testsuite errors="0" failures="0" skipped="0" tests="1" time="0.000" name="github.com/buildbuddy-io/buildbuddy/cli/arg.TestReverse" timestamp="2026-08-27T12:59:15.019Z">
      		<testcase classname="arg" name="TestReverse" time="0.000"></testcase>
      	</testsuite>
      	<testsuite errors="0" failures="0" skipped="0" tests="1" time="0.000" name="github.com/buildbuddy-io/buildbuddy/cli/arg.TestReverseTwiceIsIdentity" timestamp="2026-08-27T12:59:15.019Z">
      		<testcase classname="arg" name="TestReverseTwiceIsIdentity" time="0.000"></testcase>
      	</testsuite>
      	<testsuite errors="0" failures="0" skipped="0" tests="1" time="0.000" name="github.com/buildbuddy-io/buildbuddy/cli/arg.TestUpperSkippedExample" timestamp="2026-08-27T12:59:15.019Z">
      		<testcase classname="arg" name="TestUpperSkippedExample" time="0.000"></testcase>
      	</testsuite>
      </testsuites>
    """.trimIndent()

    val client = MockBuildTaskEventsHandler()
    val notifier = BspClientTestNotifier(client)

    val goTargetKind = TargetKindService.getInstance().findPredefinedRule("go_test")
    TestXmlParser(notifier).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, testXml), goTargetKind)

    val expectedTestHints = listOf(
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestAdd",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestAdd",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestAddTableDriven",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestAddTableDriven",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestAddTableDriven/mixed",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestAddTableDriven/negative",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestAddTableDriven/positive",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestAddTableDriven/zeros",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestIsPrime",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestIsPrime",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestReverse",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestReverse",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestReverseTwiceIsIdentity",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestReverseTwiceIsIdentity",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestUpperSkippedExample",
      "gotest://github.com/buildbuddy-io/buildbuddy/cli/arg#TestUpperSkippedExample",
    )
    client.taskStartCalls.map { (it.data as TestStart).locationHint } shouldBe expectedTestHints
  }

  private fun writeTempFile(tempDir: Path, contents: String): Path {
    val tempFile = tempDir.resolve("tempFile.xml")
    tempFile.writeText(contents)
    return tempFile
  }
}
