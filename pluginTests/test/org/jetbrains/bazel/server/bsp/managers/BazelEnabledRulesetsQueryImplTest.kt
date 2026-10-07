package org.jetbrains.bazel.server.bsp.managers

import com.intellij.aspect.lib.Rules
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.bazelrunner.BazelProcess
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.ModuleResolver
import org.jetbrains.bazel.bazelrunner.mockBazelProcessLauncher
import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.languages.projectview.ENABLED_RULES_KEY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.enabledRules
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.sync.workspace.projectTree.BazelRunnerSpyStubbingHelper
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bsp.protocol.TaskGroupId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import kotlin.io.path.Path

@BazelTestApplication
class BazelEnabledRulesetsQueryImplTest {
  @Test
  fun `returns the enabled rules as they are`() {
    fetch("rules_java", "rules_go").shouldContainExactly("rules_java", "rules_go")
  }

  @Test
  fun `adds the rules that an enabled rule needs`() {
    fetch("rules_kotlin").shouldContainExactly("rules_kotlin", "rules_java")
  }

  @Test
  fun `does not duplicate a needed rule that is also enabled`() {
    fetch("rules_java", "rules_kotlin", "rules_scala").shouldContainExactly("rules_java", "rules_kotlin", "rules_scala")
  }

  @Test
  fun `qa excluded Kotlin stays disabled with 64 enabled repositories`(): Unit = timeoutRunBlocking {
    checkExcludedKotlin(ModuleResolver.USE_ALL_THRESHOLD-1)
  }

  @Test
  fun `qa excluded Kotlin stays disabled with 65 enabled repositories`(): Unit = timeoutRunBlocking {
    checkExcludedKotlin(ModuleResolver.USE_ALL_THRESHOLD+1)
  }

  private suspend fun checkExcludedKotlin(count: Int) {
    val selected = listOf("rules_java") + (1 until count).map { "rules_extra_$it" }
    val projectView = ProjectView(mapOf(ENABLED_RULES_KEY to selected), emptyList())
    val externalNames = BazelEnabledRulesetsQueryImpl(projectView.enabledRules).fetchExternalRulesetNames()
    externalNames shouldNotContain "rules_kotlin"
    val runner = spy(BazelRunner(null, Path("workspaceRoot"), mockBazelProcessLauncher, Path("bazel")))
    val process = mock(BazelProcess::class.java)
    var command = emptyList<String>()
    `when`(process.waitAndGetResult()).thenAnswer {
      command = BazelRunnerSpyStubbingHelper.captureBazelCommandFromMock(runner).buildExecutionDescriptor().command
      val returnedNames = selected + if ("--all_repos" in command) listOf("rules_kotlin") else emptyList()
      val stdout = returnedNames.joinToString("\n") { name ->
        """{"canonicalName":"$name+","moduleKey":"$name@1.0","repoRuleName":"http_archive","attribute":[{"name":"urls","stringListValue":["https://github.com/bazelbuild/$name/releases/download/1.0/$name.tar.gz"]},{"name":"strip_prefix","stringValue":""}]}"""
      }
      BazelProcessResult(output(stdout), output(""), 0)
    }
    BazelRunnerSpyStubbingHelper.stubRunBazelCommand(runner, process)
    val info = BazelInfo.DEFAULT.copy(release = BazelRelease(9), isWorkspaceEnabled = false)
    val definitions = ModuleResolver(runner, projectView, TaskGroupId.EMPTY.task("qa-enabled-rules"))
      .resolveModules(externalNames.map { "@@$it+" }, info)
    definitions.warnings.shouldBeEmpty()
    ("--all_repos" in command) shouldBe (count > 64)
    val languages = BazelBspAspectsManager(Path("workspaceRoot"), mock(ExecuteService::class.java), info.release)
      .calculateRulesets(externalNames, definitions.result, emptyList())
      .map { it.ruleset.aspectLanguage }
    println("QA count=$count all_repos=${"--all_repos" in command} selected=$externalNames languages=$languages")
    languages shouldContain Rules.JAVA
    languages shouldNotContain Rules.KOTLIN
  }

  private fun output(text: String): OutputCollector = OutputCollector().also { it.append(text.toByteArray(Charsets.UTF_8)) }

  private fun fetch(vararg enabledRules: String): List<String> =
    BazelEnabledRulesetsQueryImpl(enabledRules.toList()).fetchExternalRulesetNamesImpl()
}
