package org.jetbrains.bazel.intellij

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.java.ui.gutters.JvmTestFilterExtension
import org.jetbrains.bazel.jvm.run.JvmTestRerunExtension
import org.jetbrains.bazel.jvm.run.JvmTestRunnerExtension
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.languages.starlark.references.findReferredPackage
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bazel.run.state.HasEnv
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.target.getTargetDataForLabel
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.id
import java.nio.file.Path
import kotlin.io.path.useLines

private fun BuildTarget.usesJetBrainsTestRunner(project: Project): Boolean =
  JetBrainsTestRunner.TAG in tags ||
  usesJetBrainsTestRunnerCompat(project) ||
  JetBrainsTestRunner.Detector.anyDetects(project, id)

// only needed for some time to keep the JetBrains test runner recognizable for outdated branches (without "jetbrains_test_runner" tag)
private fun BuildTarget.usesJetBrainsTestRunnerCompat(project: Project): Boolean {
  return kind.ruleType == RuleType.TEST &&
         project.targetStorage.getTargetDataForLabel<JvmBuildTarget>(id)?.mainClass in wellKnownJetBrainsTestRunnerImpls
}

private val wellKnownJetBrainsTestRunnerImpls = setOf(
  "com.intellij.tests.JUnit5BazelRunner", // monorepo
  "jetbrains.datalore.buildScripts.JUnit5TestLauncher", // datalore
)

private fun targetsUseJetBrainsTestRunner(project: Project, targets: List<Label>): Boolean {
  if (targets.isEmpty()) return false
  val expandedTargets = targets.asSequence().flatMap { expandWildcardTarget(project, it) }
  val targetStorage = project.targetStorage
  return expandedTargets.all { label ->
    targetStorage.getTargetSummary(label)?.usesJetBrainsTestRunner(project) ?: JetBrainsTestRunner.Detector.anyDetects(project, label)
  }
}

private fun expandWildcardTarget(project: Project, target: Label): List<Label> {
  if (target.target !is AllRuleTargets) return listOf(target)
  val testableSummaries = project.targetStorage.allTestableSummaries()
  if (testableSummaries.isEmpty()) return listOf(target)  // Monorepo with JPS case
  val baseDirectory = findReferredPackage(project, target.assumeResolved())?.toNioPathOrNull()
                      ?: return listOf(target)
  return testableSummaries
    .filter { it.baseDirectory.startsWith(baseDirectory) }
    .map { it.id }
           .takeIf { it.isNotEmpty() } ?: listOf(target)
}

@ApiStatus.Internal
object JetBrainsTestRunner {

  const val TAG: String = "jetbrains_test_runner"

  internal const val IDE_SM_RUN: String = "JB_IDE_SM_RUN"

  internal const val TEST_FILTER: String = "JB_TEST_FILTER"

  internal const val TEST_UNIQUE_IDS: String = "JB_TEST_UNIQUE_IDS"

  internal fun envs(testFilter: String?): Map<String, String> = when (testFilter) {
    null -> mapOf(IDE_SM_RUN to "true")
    else -> mapOf(IDE_SM_RUN to "true", TEST_FILTER to testFilter)
  }

  internal fun setTestUniqueIds(state: BazelRunConfigurationState<*>, testUniqueIds: List<String>) {
    (state as? HasTestFilter)?.testFilter = null
    (state as? HasEnv)?.env?.envs?.let {
      it.remove(TEST_FILTER)
      it[TEST_UNIQUE_IDS] = testUniqueIds.joinToString(separator = ";")
      it[IDE_SM_RUN] = "true"
    }
  }

  internal fun getTestUniqueIds(state: BazelRunConfigurationState<*>): List<String>? {
    (state as? HasEnv)?.env?.envs?.let {
      return it[TEST_UNIQUE_IDS]?.split(";")
    }
    return null
  }

  /**
   * Recognizes targets running on the JetBrains test runner that carry no [JetBrainsTestRunner.TAG], because they are
   * absent from [org.jetbrains.bazel.target.targetStorage].
   *
   * It's only needed for monorepo with JPS - see `MonorepoJetBrainsTestRunnerDetector.kt`.
   * If it's gone this EP can be removed.
   */
  @ApiStatus.Internal
  interface Detector {
    fun usesJetBrainsTestRunner(project: Project, label: Label): Boolean

    companion object {
      val ep: ExtensionPointName<Detector> =
        ExtensionPointName.create("org.jetbrains.bazel.jetBrainsTestRunnerDetector")

      internal fun anyDetects(project: Project, label: Label): Boolean =
        ep.lazySequence().any { it.usesJetBrainsTestRunner(project, label) }
    }
  }
}

internal fun transformJetBrainsTestRunnerParams(params: TestParams): TestParams =
  params.copy(
    environmentVariables = params.environmentVariables + JetBrainsTestRunner.envs(params.testFilter),
    testFilter = null,
    streamTestOutput = true,
  )

internal class JetBrainsTestRunnerExtension : JvmTestRunnerExtension {
  override fun isApplicable(project: Project, targets: List<Label>): Boolean = targetsUseJetBrainsTestRunner(project, targets)

  override val isIdBasedTestTree: Boolean get() = true

  override val testRunnerEmitsServiceMessages: Boolean get() = true

  override fun transformTestParams(params: TestParams): TestParams = transformJetBrainsTestRunnerParams(params)

  override fun transformScriptPathEnvAndTestFilter(env: Map<String, String>, testFilter: String?): Pair<Map<String, String>, String?> =
    env + JetBrainsTestRunner.envs(testFilter) to null

  override fun createTaskListener(handler: BazelProcessHandler, coverageReportListener: ((Path) -> Unit)?): BazelTaskListener =
    JetBrainsTestRunnerTaskListener(handler)
}

internal class JetBrainsTestRerunExtension : JvmTestRerunExtension {
  override fun isApplicable(configuration: BazelRunConfiguration): Boolean =
    targetsUseJetBrainsTestRunner(configuration.project, configuration.targets)

  override fun setTestsToRerun(state: BazelRunConfigurationState<*>, tests: List<AbstractTestProxy>): Boolean {
    val testIds = tests.getTestIds()
    if (testIds.isEmpty()) return false
    JetBrainsTestRunner.setTestUniqueIds(state, testIds)
    return true
  }

  override fun isRerunOf(state: BazelRunConfigurationState<*>, tests: List<AbstractTestProxy>): Boolean {
    val testIds = JetBrainsTestRunner.getTestUniqueIds(state) ?: return false
    return testIds.isNotEmpty() && tests.getTestIds() == testIds
  }
}

@ApiStatus.Internal
class JetBrainsTestFilterExtension : JvmTestFilterExtension {
  override fun isApplicable(project: Project, target: BuildTarget): Boolean = target.usesJetBrainsTestRunner(project)

  override fun getTestFilter(className: String, method: PsiMethod?): String =
    if (method == null) className else "$className:${method.name}:${method.getMethodParameterTypes()}"
}

private fun List<AbstractTestProxy>.getTestIds(): List<String> =
  filter { it.metainfo == "test" }
    .mapNotNull { it.getUserData(SMTestProxy.NODE_ID) }

/**
 * See [JUnit docs](https://docs.junit.org/5.2.0/api/org/junit/platform/engine/discovery/MethodSelector.html#getMethodParameterTypes())
 */
private fun PsiMethod.getMethodParameterTypes(): String =
  this.parameterList.parameters.map { it.type }.mapNotNull { type ->
    if (type is PsiClassType) {
      // canonicalText will include type arguments if they are present, avoid that in simple cases
      type.resolve()?.qualifiedName
    }
    else {
      type.canonicalText
    }
  }.joinToString(separator = ",")

internal class JetBrainsTestSuiteHandler : BazelRunHandler {
  override val state: GenericTestState = GenericTestState()

  override val name: String
    get() = "test_suite (JetBrains test runner)"

  override val isTestHandler: Boolean
    get() = true

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    JetBrainsTestSuiteState(environment, state)

  internal class JetBrainsTestSuiteHandlerProvider : RunHandlerProvider {
    override val id: String
      get() = "JetBrainsTestSuiteHandlerProvider"

    override fun canRun(
      project: Project,
      targets: List<BuildTarget>,
    ): Boolean =
      targets.all { it.kind.kind == "test_suite" && it.usesJetBrainsTestRunner(project) }

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JetBrainsTestSuiteHandler()
  }
}

internal class JetBrainsTestSuiteState(environment: ExecutionEnvironment, state: GenericTestState) :
  BazelTestCommandLineState(environment, state) {
  override val isIdBasedTestTree: Boolean get() = true
  override val testRunnerEmitsServiceMessages: Boolean get() = true

  override fun transformTestParams(params: TestParams): TestParams =
    transformJetBrainsTestRunnerParams(params)

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = JetBrainsTestRunnerTaskListener(handler)
}

private const val TEAMCITY_PREFIX = "##teamcity[test"
private const val TEST_NAME_TAG = " name='"
private const val JAVA_TEST_SCHEMA = "java:test://"

internal class JetBrainsTestRunnerTaskListener(handler: BazelProcessHandler) : BazelRunTaskListener(handler) {
  init {
    handler.notifyTextAvailable("##teamcity[enteredTheMatrix]\n", ProcessOutputType.STDOUT)
    handler.notifyTextAvailable("##teamcity[testingStarted]\n", ProcessOutputType.STDOUT)
  }

  override fun onCachedTestLog(testLog: Path) {
    testLog.useLines { lines ->
      lines.map { line ->
        markTestNameAsCached(line)
      }.forEach { line ->
        handler.notifyTextAvailable(line + "\n", ProcessOutputType.STDOUT)
      }
    }
  }

  private fun markTestNameAsCached(line: String): String {
    if (!line.startsWith(TEAMCITY_PREFIX)) return line
    if (JAVA_TEST_SCHEMA !in line) return line
    val nameStart = line.indexOf(TEST_NAME_TAG)
    if (nameStart == -1) return line
    val nameEnd = line.indexOf('\'', startIndex = nameStart + TEST_NAME_TAG.length)
    return line.substring(0 until nameEnd) + " (cached)" + line.substring(nameEnd)
  }
}
