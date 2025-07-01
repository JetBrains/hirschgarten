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
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.sync.BazelBinPathService
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.includesGo
import org.jetbrains.bazel.server.connection.connection
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

private val PROVIDER_ID =
  Key.create<BazelGoCalculateExecutableInfoBeforeRunTaskProvider.Task>(
    "BazelGoCalculateExecutableInfoBeforeRunTaskProvider",
  )

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
class BazelGoCalculateExecutableInfoBeforeRunTaskProvider :
  BeforeRunTaskProvider<BazelGoCalculateExecutableInfoBeforeRunTaskProvider.Task>() {
  class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = "Bazel Go Calculate Executable Info"

  override fun createTask(runConfiguration: RunConfiguration): Task? {
    if (!BazelFeatureFlags.isGoSupportEnabled) return null
    if (runConfiguration !is BazelRunConfiguration) return null
    return Task()
  }

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
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
    val success =
      runBlocking {
        val result =
          withBackgroundProgress(project, "Preparing for debugging go target $target") {
            project.connection.runWithServer { server ->
              server.buildTargetRun(
                RunParams(
                  target = runConfiguration.targets.single(),
                  originId = "",
                  workingDirectory = project.rootDir.path,
                  arguments = emptyList(),
                  environmentVariables = emptyMap(),
                  additionalBazelParams =
                    "--script_path=$scriptPath " +
                      "--dynamic_mode=off " +
                      "--test_sharding_strategy=disabled " +
                      "--compilation_mode=dbg " +
                      "--test_env=GO_TEST_WRAP_TESTV=1",
                ),
              )
            }
          }
        if (result.statusCode != BazelStatus.SUCCESS) {
          BazelBalloonNotifier.error(
            "Cannot calculate executable info for Go targets",
            "The request failed with status code ${result.statusCode}. \nPlease try again.",
          )
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
      throw ExecutionException("Could not read script_path: $scriptPath", e)
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
        throw ExecutionException("Failed to parse args in script_path: $scriptPath")
      }
      // Make paths used for runfiles discovery absolute as the working directory is changed below.
      envVars["TEST_SRCDIR"] = workspaceRoot.resolve(testScrDir.groupValues[1]).toString()
      val runfilesVars = RUNFILES_VAR.findAll(text)
      for (runfilesVar in runfilesVars) {
        val envKey = "RUNFILES_${runfilesVar.groupValues[1]}"
        val envVal: String = runfilesVar.groupValues[2]
        if ("RUNFILES_MANIFEST_ONLY" == envKey && TRUTHY_ENV_VALUES_LOWER.contains(envVal.trim { it <= ' ' }.lowercase(Locale.getDefault()))
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
        throw ExecutionException("Failed to parse args in script_path: $scriptPath")
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
