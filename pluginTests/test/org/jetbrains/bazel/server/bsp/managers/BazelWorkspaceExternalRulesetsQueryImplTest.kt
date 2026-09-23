package org.jetbrains.bazel.server.bsp.managers

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.bazelrunner.BazelProcess
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.mockBazelProcessLauncher
import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.server.bsp.managers.BazelWorkspaceExternalRulesetsQueryImpl.Companion.parseWorkspaceExternalRulesetNames
import org.jetbrains.bazel.sync.BazelEnvironmentService
import org.jetbrains.bazel.sync.workspace.projectTree.BazelRunnerSpyStubbingHelper
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.test.framework.enableGoHighlighting
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CachedTestLog
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.MessageType
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskStartParams
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
internal class BazelWorkspaceExternalRulesetsQueryImplTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/go_workspace_git_repository",
    bazelVersion = "7.7.1",
    configure = { it.enableGoHighlighting() },
  )

  /** Records the log messages that the query reports to the sync console. */
  private class RecordingTaskEventsHandler : BazelTaskEventsHandler {
    val logMessages = mutableListOf<LogMessageParams>()

    override fun onBuildLogMessage(params: LogMessageParams) {
      logMessages.add(params)
    }

    override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {}
    override fun onBuildTaskStart(params: TaskStartParams) {}
    override fun onBuildTaskFinish(params: TaskFinishParams) {}
    override fun onPublishCoverageReport(report: CoverageReport) {}
    override fun onCachedTestLog(testLog: CachedTestLog) {}
  }

  private val emptyProjectView = ProjectView(sections = emptyMap(), imports = emptyList())

  private val taskId = TaskGroupId.EMPTY.task("")

  // ---------------------------------------------------------------------------------------------
  // A real Bazel run against the synced test project
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisabledOnOs(OS.WINDOWS) // bazel failed to get go rules
  fun testCustomAspectGeneratedClassResolves() = timeoutRunBlocking(timeout = 5.minutes) {
    val project = fixture.project
    val workspaceIndex = WorkspaceFileIndex.getInstance(project)
    readAction { workspaceIndex.isIndexable(project.rootDir.findFileByRelativePath("main.go")!!) shouldBe true }

    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("main.go")
    }
  }

  @Test
  @DisabledOnOs(OS.WINDOWS) // bazel failed to get go rules
  fun `bazel query returns the git_repository ruleset of the WORKSPACE file`(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    val project = fixture.project
    val projectRoot = project.rootDir.toNioPath()
    val projectView = ProjectViewService.getInstance(project).projectView
    val bazelProcessLauncher =
      BazelProcessLauncherProvider.getInstance()
        .createBazelProcessLauncher(projectRoot, BazelEnvironmentService.getInstance(project).getEnvironment())
    val bazelRunner = BazelRunner.create(project, null, projectRoot, bazelProcessLauncher, projectView)
    val taskEventsHandler = RecordingTaskEventsHandler()

    val rulesetNames =
      BazelWorkspaceExternalRulesetsQueryImpl(taskId, bazelRunner, isWorkspaceEnabled = true, taskEventsHandler, projectView)
        .fetchExternalRulesetNames()

    // The macros `go_rules_dependencies()` and `maybe()` create the other http_archive rules, so they are not rulesets.
    rulesetNames shouldContainExactly listOf("io_bazel_rules_go")
    taskEventsHandler.logMessages.shouldBeEmpty()
  }

  // ---------------------------------------------------------------------------------------------
  // The command and the failure path, with a stubbed BazelRunner
  // ---------------------------------------------------------------------------------------------

  @Test
  fun `query asks for the external targets in the streamed json proto format`(): Unit = timeoutRunBlocking {
    val bazelRunner = stubbedBazelRunner(stdout = gitRepositoryLine("rules_x"), exitCode = 0)

    val rulesetNames =
      BazelWorkspaceExternalRulesetsQueryImpl(taskId, bazelRunner, isWorkspaceEnabled = true, RecordingTaskEventsHandler(), emptyProjectView)
        .fetchExternalRulesetNames()

    rulesetNames shouldContainExactly listOf("rules_x")
    val commandLine = BazelRunnerSpyStubbingHelper.captureBazelCommandFromMock(bazelRunner).buildExecutionDescriptor().command
    commandLine shouldContain "query"
    commandLine shouldContain "--output=streamed_jsonproto"
    commandLine shouldNotContain "--output=xml"
    commandLine.last().removeSurrounding("\"") shouldEndWith "//external:*"
  }

  @Test
  fun `failed query returns no rulesets and reports a warning`(): Unit = timeoutRunBlocking {
    val stderr = "ERROR: no such package 'external': WORKSPACE file is missing"
    val bazelRunner = stubbedBazelRunner(stdout = "", stderr = stderr, exitCode = 1)
    val taskEventsHandler = RecordingTaskEventsHandler()

    val rulesetNames =
      BazelWorkspaceExternalRulesetsQueryImpl(taskId, bazelRunner, isWorkspaceEnabled = true, taskEventsHandler, emptyProjectView)
        .fetchExternalRulesetNames()

    rulesetNames.shouldBeEmpty()
    taskEventsHandler.logMessages.size shouldBe 1
    taskEventsHandler.logMessages.single().type shouldBe MessageType.WARNING
    taskEventsHandler.logMessages.single().message shouldContain stderr
  }

  @Test
  fun `disabled WORKSPACE skips the bazel call`(): Unit = timeoutRunBlocking {
    // `mockBazelProcessLauncher` throws on a launch, so a bazel call fails the test.
    val bazelRunner = BazelRunner(null, Path("workspaceRoot"), mockBazelProcessLauncher, Path("bazel"))

    val rulesetNames =
      BazelWorkspaceExternalRulesetsQueryImpl(taskId, bazelRunner, isWorkspaceEnabled = false, RecordingTaskEventsHandler(), emptyProjectView)
        .fetchExternalRulesetNames()

    rulesetNames.shouldBeEmpty()
  }

  // ---------------------------------------------------------------------------------------------
  // The ndjson parser
  // ---------------------------------------------------------------------------------------------

  @Test
  fun `git_repository rule is a ruleset`() {
    parseWorkspaceExternalRulesetNames(listOf(gitRepositoryLine("io_bazel_rules_go"))) shouldContainExactly listOf("io_bazel_rules_go")
  }

  @Test
  fun `http_archive rule without a generator function is a ruleset`() {
    parseWorkspaceExternalRulesetNames(listOf(httpArchiveLine("rules_java", generatorFunction = null))) shouldContainExactly listOf("rules_java")
  }

  @Test
  fun `http_archive rule with the default empty generator function is a ruleset`() {
    // `--proto:default_values` is on by default, so Bazel prints the attribute with an empty value.
    parseWorkspaceExternalRulesetNames(listOf(httpArchiveLine("rules_java", generatorFunction = ""))) shouldContainExactly listOf("rules_java")
  }

  @Test
  fun `http_archive rule from an http_archive wrapper macro is a ruleset`() {
    parseWorkspaceExternalRulesetNames(listOf(httpArchiveLine("rules_kotlin", generatorFunction = "maybe_http_archive"))) shouldContainExactly
      listOf("rules_kotlin")
  }

  @Test
  fun `http_archive rule from another macro is not a ruleset`() {
    parseWorkspaceExternalRulesetNames(listOf(httpArchiveLine("bazel_gazelle", generatorFunction = "go_rules_dependencies"))).shouldBeEmpty()
  }

  @Test
  fun `other repository rules are not rulesets`() {
    val lines =
      listOf(
        ruleLine("local_repository", "my_local", attributes = nameAttribute("my_local")),
        ruleLine("new_local_repository", "my_new_local", attributes = nameAttribute("my_new_local")),
        ruleLine("maven_install", "maven", attributes = nameAttribute("maven")),
        ruleLine("bind", "bound", attributes = nameAttribute("bound")),
      )
    parseWorkspaceExternalRulesetNames(lines).shouldBeEmpty()
  }

  @Test
  fun `targets that are not rules and rules without a name attribute are skipped`() {
    val lines =
      listOf(
        """{"type":"SOURCE_FILE","sourceFile":{"name":"//external:WORKSPACE","location":"/w/WORKSPACE:1:1"}}""",
        ruleLine("git_repository", "nameless", attributes = ""),
        gitRepositoryLine("rules_x"),
      )
    parseWorkspaceExternalRulesetNames(lines) shouldContainExactly listOf("rules_x")
  }

  @Test
  fun `blank lines and both line endings of ndjson are tolerated`() {
    val stdout = gitRepositoryLine("rules_a") + "\r\n\r\n" + httpArchiveLine("rules_b", generatorFunction = null) + "\n\n"
    val result = BazelProcessResult(outputCollector(stdout), outputCollector(""), 0)

    parseWorkspaceExternalRulesetNames(result.stdoutLines) shouldContainExactly listOf("rules_a", "rules_b")
  }

  @Test
  fun `malformed line is skipped and the other rules are kept`() {
    val lines =
      listOf(
        gitRepositoryLine("rules_a"),
        """{"type":"RULE","rule":{"name":"//external:broken","ruleClass":"git_repository","attribute":[""",
        "not json at all",
        """{"type":"RULE","rule":"unexpected string"}""",
        gitRepositoryLine("rules_b"),
      )
    parseWorkspaceExternalRulesetNames(lines) shouldContainExactly listOf("rules_a", "rules_b")
  }

  @Test
  fun `output far above the xml document limit is parsed in full and in order`() {
    val ruleCount = 5_000
    val lines = (0 until ruleCount).map { index -> if (index % 2 == 0) gitRepositoryLine("git_$index") else httpArchiveLine("http_$index", null) }
    lines.sumOf { it.length } shouldBeGreaterThan 100_000

    val expected = (0 until ruleCount).map { index -> if (index % 2 == 0) "git_$index" else "http_$index" }
    parseWorkspaceExternalRulesetNames(lines) shouldContainExactly expected
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  /** A [BazelRunner] spy whose `runBazelCommand` returns a finished process with the given output. */
  private fun stubbedBazelRunner(stdout: String, stderr: String = "", exitCode: Int): BazelRunner {
    val runner = spy(BazelRunner(null, Path("workspaceRoot"), mockBazelProcessLauncher, Path("bazel")))
    val result = BazelProcessResult(outputCollector(stdout), outputCollector(stderr), exitCode)
    val process = mock(BazelProcess::class.java)
    runBlocking {
      `when`(process.waitAndGetResult()).thenReturn(result)
    }
    BazelRunnerSpyStubbingHelper.stubRunBazelCommand(runner, process)
    return runner
  }

  private fun outputCollector(text: String): OutputCollector = OutputCollector().also { it.append(text.toByteArray(Charsets.UTF_8)) }

  /** One `blaze_query.Target` line, in the shape that `bazel query --output=streamed_jsonproto` prints. */
  private fun ruleLine(ruleClass: String, targetName: String, attributes: String): String =
    """{"type":"RULE","rule":{"name":"//external:$targetName","ruleClass":"$ruleClass","location":"/workspace/WORKSPACE:3:15","attribute":[$attributes]}}"""

  private fun nameAttribute(name: String): String =
    """{"name":"name","type":"STRING","stringValue":"$name","explicitlySpecified":true,"nodep":false}"""

  private fun generatorFunctionAttribute(generatorFunction: String): String =
    """{"name":"generator_function","type":"STRING","stringValue":"$generatorFunction","explicitlySpecified":false,"nodep":false}"""

  private fun gitRepositoryLine(name: String): String =
    ruleLine(
      "git_repository",
      name,
      attributes =
        nameAttribute(name) + "," +
        """{"name":"remote","type":"STRING","stringValue":"https://github.com/example/$name.git","explicitlySpecified":true,"nodep":false},""" +
        generatorFunctionAttribute(""),
    )

  private fun httpArchiveLine(name: String, generatorFunction: String?): String =
    ruleLine(
      "http_archive",
      name,
      attributes =
        listOfNotNull(
          nameAttribute(name),
          """{"name":"urls","type":"STRING_LIST","stringListValue":["https://example.com/$name.tar.gz"],"explicitlySpecified":true,"nodep":false}""",
          generatorFunction?.let { generatorFunctionAttribute(it) },
        ).joinToString(","),
    )
}
