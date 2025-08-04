package org.jetbrains.bazel.golang.debug

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.sync.BazelBinPathService
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier
import org.jetbrains.bsp.protocol.RunParams
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import kotlin.io.path.name
import kotlin.io.path.readText

// Matches TEST_SRCDIR=<dir>
private val TEST_SRCDIR = "TEST_SRCDIR=(\\S+)".toRegex()

// Matches RUNFILES_<NAME>=<value>
private val RUNFILES_VAR = "RUNFILES_([A-Z_]+)=(\\S+)".toRegex()

// Matches TEST_TARGET=//<package_path>:<target>
private val TEST_TARGET = "TEST_TARGET=//([^:]*):(\\S+)".toRegex()

// Matches a space-delimited arg list. Supports wrapping arg in single quotes.
private val ARGS = "([^\']\\S*|\'.+?\')\\s*".toRegex()

// The actual Bazel shell script uses "1", which is considered as true.
// For robustness, we include other common representations of true.
private val TRUTHY_ENV_VALUES_LOWER = listOf("true", "1", "yes", "on")

private val POP_UP_MESSAGE_ENABLE_SYMLINKS: String =
  """
  Please enable symlink support. Add the following lines to your .bazelrc file:
  startup --windows_enable_symlinks
  build --enable_runfiles
  
  Refer to the online documentation for Using Bazel on Windows: https://bazel.build/configure/windows.
  
  """.trimIndent()

/**
 * this class is inspired from [this code snippet](https://github.com/bazelbuild/intellij/blob/master/golang/src/com/google/idea/blaze/golang/run/BlazeGoRunConfigurationRunner.java)
 */
internal sealed class BazelGoBeforeRunTaskProvider<T : BeforeRunTask<T>> : BeforeRunTaskProvider<T>() {
  override fun createTask(runConfiguration: RunConfiguration): T? {
    if (!BazelFeatureFlags.isGoSupportEnabled) return null
    if (runConfiguration !is BazelRunConfiguration) return null
    return createTaskInstance()
  }

  abstract fun createTaskInstance(): T

  open fun additionalBazelParams(runConfiguration: BazelRunConfiguration): List<String> = emptyList()

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: T,
  ): Boolean {
    val runConfiguration = environment.runProfile as BazelRunConfiguration
    // skipping this task for non-debugging run config
    if (environment.executor !is DefaultDebugExecutor) return true
    val scriptPath = createTempScriptFile()
    val project = environment.project
    val targetUtils = project.targetUtils
    val targetInfos = runConfiguration.targets.mapNotNull { targetUtils.getBuildTargetForLabel(it) }
    if (targetInfos.any {
        !it.kind.includesGo() || (it.kind.ruleType != RuleType.TEST && it.kind.ruleType != RuleType.BINARY)
      }
    ) {
      return false
    }
    val target = runConfiguration.targets.single()

    val bazelParams =
      listOf(
        "--script_path=$scriptPath",
        "--dynamic_mode=off",
        "--test_sharding_strategy=disabled",
        "--compilation_mode=dbg",
      ) + additionalBazelParams(runConfiguration)

    val success =
      runBlocking {
        val result =
          withBackgroundProgress(project, BazelPluginBundle.message("go.debug.background.progress.start.title", target)) {
            val params =
              RunParams(
                target = runConfiguration.targets.single(),
                originId = "",
                workingDirectory = project.rootDir.path,
                arguments = emptyList(),
                environmentVariables = emptyMap(),
                additionalBazelParams = bazelParams.joinToString(" "),
              )
            BazelWorkspaceResolveService
              .getInstance(project)
              .withEndpointProxy { it.buildTargetRun(params) }
          }
        if (result.statusCode != BazelStatus.SUCCESS) {
          BazelBalloonNotifier.error(
            "Cannot calculate executable info for Go targets",
            "The request failed with status code ${result.statusCode}. \nPlease try again.",
          )
          return@runBlocking false
        }
        environment.getCopyableUserData(EXECUTABLE_KEY).set(parseScriptPathFile(scriptPath, project))
        return@runBlocking true
      }
    return success
  }

  private fun createTempScriptFile(): Path =
    Files.createTempFile(Paths.get(FileUtilRt.getTempDirectory()), "bazel-script-", "").also { it.toFile().deleteOnExit() }

  @Throws(ExecutionException::class)
  private fun parseScriptPathFile(scriptPath: Path, project: Project): ExecutableInfo {
    val text: String
    try {
      text = scriptPath.readText(charset = Charsets.UTF_8)
    } catch (e: IOException) {
      throw ExecutionException(BazelPluginBundle.message("go.before.run.error.invalid.script.path", scriptPath), e)
    }
    val lastLine: String = text.split("\n").last()
    val argsMatcher = ARGS.findAll(lastLine.trim { it <= ' ' })
    var args = argsMatcher.map { it.groupValues[1].trimStart('\'').trimEnd('\'') }.toList()
    val envVars = mutableMapOf<String, String>()
    val binary: File
    val workingDir: File
    val testScrDir = TEST_SRCDIR.find(text)
    val workspaceRoot = project.rootDir.toNioPath()
    val execRoot = Path.of(BazelBinPathService.getInstance(project).bazelExecPath)
    if (testScrDir != null) {
      // Format is <wrapper-script> <executable> arg0 arg1 arg2 ... argN "@"
      if (args.size < 3) {
        throw ExecutionException(BazelPluginBundle.message("go.before.run.error.parsing.script.failure", scriptPath))
      }
      // Make paths used for runfiles discovery absolute as the working directory is changed below.
      envVars["TEST_SRCDIR"] = workspaceRoot.resolve(testScrDir.groupValues[1]).toString()
      val runfilesVars = RUNFILES_VAR.findAll(text)
      for (runfilesVar in runfilesVars) {
        val envKey = "RUNFILES_${runfilesVar.groupValues[1]}"
        val envVal: String = runfilesVar.groupValues[2]
        if ("RUNFILES_MANIFEST_ONLY" == envKey &&
          TRUTHY_ENV_VALUES_LOWER.contains(
            envVal.trim { it <= ' ' }.lowercase(Locale.getDefault()),
          )
        ) {
          throw ExecutionException(POP_UP_MESSAGE_ENABLE_SYMLINKS)
        }
        envVars[envKey] = workspaceRoot.resolve(envVal).toString()
      }
      val workspaceName = execRoot.name
      binary =
        Paths
          .get(
            project.rootDir.canonicalPath,
            testScrDir.groupValues[1],
            workspaceName,
            args[1],
          ).toFile()

      val testTarget = TEST_TARGET.find(text)
      if (testTarget != null) {
        val packagePath = testTarget.groupValues[1]
        workingDir =
          workspaceRoot
            .resolve(testScrDir.groupValues[1])
            .resolve(workspaceName)
            .resolve(packagePath)
            .toFile()
      } else {
        workingDir = workspaceRoot.toFile()
      }

      // Remove everything except the args.
      args = args.drop(2).dropLast(1)
    } else {
      // Format is <executable> [arg0 arg1 arg2 ... argN] "@"
      if (args.size < 2) {
        throw ExecutionException(BazelPluginBundle.message("go.before.run.error.args.parsing.failure", scriptPath))
      }
      binary = File(args[0])
      workingDir = getWorkingDirectory(workspaceRoot.toFile(), binary)
      // Remove everything except the args.
      args = args.drop(1)
    }
    return ExecutableInfo(binary, workingDir, args, envVars)
  }

  /**
   * Similar to [com.google.idea.blaze.python.run.BlazePyRunConfigurationRunner].
   *
   *
   * Working directory should be the runfiles directory of the debug executable.
   *
   *
   * If the runfiles directory does not exist (unlikely) fall back to workspace root.
   */
  private fun getWorkingDirectory(root: File, executable: File): File {
    val workspaceName = root.name
    val expectedPath = File(executable.path + ".runfiles", workspaceName)
    if (expectedPath.isDirectory) return expectedPath
    return root
  }
}

private const val TEST_PROVIDER_NAME = "BazelGoTestBeforeRunTaskProvider"

private val TEST_PROVIDER_ID = Key.create<BazelGoTestBeforeRunTaskProvider.Task>(TEST_PROVIDER_NAME)

internal class BazelGoTestBeforeRunTaskProvider : BazelGoBeforeRunTaskProvider<BazelGoTestBeforeRunTaskProvider.Task>() {
  class Task : BeforeRunTask<Task>(TEST_PROVIDER_ID)

  override fun createTaskInstance(): Task = Task()

  override fun additionalBazelParams(runConfiguration: BazelRunConfiguration): List<String> =
    listOfNotNull(
      runConfiguration.extractTestFilter(),
      "--test_env=GO_TEST_WRAP_TESTV=1",
    ) + bazelParamsFromState(runConfiguration)

  private fun bazelParamsFromState(runConfiguration: BazelRunConfiguration): List<String> =
    (runConfiguration.handler?.state as? GenericTestState)
      ?.additionalBazelParams
      ?.split(" ")
      ?.filter { it.isNotBlank() }
      .orEmpty()

  private fun BazelRunConfiguration.extractTestFilter(): String? {
    val rawTestFilter = (handler?.state as? GenericTestState)?.testFilter
    if (rawTestFilter.isNullOrEmpty()) return null
    return "--test_filter=$rawTestFilter"
  }

  override fun getId(): Key<Task> = TEST_PROVIDER_ID

  override fun getName() = TEST_PROVIDER_NAME
}

private const val BINARY_PROVIDER_NAME = "BazelGoBinaryBeforeRunTaskProvider"

private val BINARY_PROVIDER_ID = Key.create<BazelGoBinaryBeforeRunTaskProvider.Task>(BINARY_PROVIDER_NAME)

internal class BazelGoBinaryBeforeRunTaskProvider : BazelGoBeforeRunTaskProvider<BazelGoBinaryBeforeRunTaskProvider.Task>() {
  class Task : BeforeRunTask<Task>(BINARY_PROVIDER_ID)

  override fun additionalBazelParams(runConfiguration: BazelRunConfiguration): List<String> =
    (runConfiguration.handler?.state as? GenericRunState)
      ?.additionalBazelParams
      ?.split(" ")
      ?.filter { it.isNotBlank() }
      .orEmpty()

  override fun createTaskInstance(): Task = Task()

  override fun getId(): Key<Task> = BINARY_PROVIDER_ID

  override fun getName() = BINARY_PROVIDER_NAME
}
