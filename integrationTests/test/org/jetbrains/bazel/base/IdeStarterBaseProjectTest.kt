package org.jetbrains.bazel.base

import com.intellij.driver.client.Driver
import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.client.utility
import com.intellij.driver.model.OnDispatcher
import com.intellij.driver.model.RdTarget
import com.intellij.driver.sdk.Project
import com.intellij.driver.sdk.ProjectManager
import com.intellij.driver.sdk.VirtualFile
import com.intellij.driver.sdk.openEditor
import com.intellij.driver.sdk.openToolWindow
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.UiText.Companion.asString
import com.intellij.driver.sdk.ui.components.UiComponent
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.mainToolbar
import com.intellij.driver.sdk.ui.components.common.restartDebugButton
import com.intellij.driver.sdk.ui.components.common.toolwindows.DebugToolWindowUi
import com.intellij.driver.sdk.ui.components.common.toolwindows.debugToolWindow
import com.intellij.driver.sdk.ui.components.elements.dialog
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.driver.sdk.waitFor
import com.intellij.driver.sdk.waitForCodeAnalysis
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.teamcity.TeamCityCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.execute
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.path.GlobalPaths
import com.intellij.ide.starter.process.ProcessInfo
import com.intellij.ide.starter.process.findAndKillProcesses
import com.intellij.ide.starter.process.getProcessList
import com.intellij.ide.starter.process.ProcessKiller.killProcesses
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.runner.Starter
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.assertCaretPosition
import com.intellij.tools.ide.performanceTesting.commands.assertCurrentFile
import com.intellij.tools.ide.performanceTesting.commands.delay
import com.intellij.tools.ide.performanceTesting.commands.goToDeclaration
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.IdeStarterBazelProject
import org.jetbrains.bazel.performanceImpl.FileKindCheck
import org.jetbrains.bazel.test.compat.IntegrationTestCompat
import org.jetbrains.bazel.testing.IS_IN_IDE_STARTER_TEST
import org.jetbrains.bazel.tests.combined.VirtualFileManager
import org.jetbrains.intellij.build.dependencies.JdkDownloader.OS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.function.Predicate
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ExtendWith(IdeStarterBaseProjectTest.StopOwnedProcessesAfterClass::class)
abstract class IdeStarterBaseProjectTest {
  protected open val timeout: Duration
    get() = (System.getProperty("bazel.ide.starter.test.timeout.seconds")?.toIntOrNull() ?: 1200).seconds

  protected var criticalProblemOccurred = false

  private val contexts = mutableListOf<IDETestContext>()

  /** The output bases of [contexts]. A context of a shared checkout writes the same `user.bazelrc` again, so read each one only once. */
  private val ownedOutputBases = mutableSetOf<String>()

  internal val ideStaysOpenBetweenTests: Boolean
    get() = this::class.java.getAnnotation(TestInstance::class.java)?.value == TestInstance.Lifecycle.PER_CLASS

  protected fun isDriverConnected(bgRun: BackgroundRun): Boolean =
    bgRun.driver.isConnected

  protected fun withDriver(bgRun: BackgroundRun, block: Driver.() -> Unit) {
    if (bgRun.driver.isConnected) {
      bgRun.driver.withContext { block() }
    } else if (!criticalProblemOccurred) {
      criticalProblemOccurred = true
      error("IDE is not connected")
    }
  }

  protected fun createContext(
    projectName: String,
    case: TestCase<*>,
    pluginZipOverride: Path? = null,
  ): IDETestContext {
    IntegrationTestCompat.onPreCreateContext()
    hideOuterBazeliskrcFromProjects()
    val ctx = Starter.newContext(projectName, case)
    ownedOutputBases.add(HermeticityGuard.assertContextIsolated(ctx, ownsProjectDir = case.projectInfo is IdeStarterBazelProject))
    contexts.add(ctx)
    if (pluginZipOverride != null) {
      ctx.pluginConfigurator.installPluginFromPath(pluginZipOverride)
    } else {
      IntegrationTestCompat.onPostCreateContext(ctx)
    }

    return ctx
      .executeRightAfterIdeOpened(true)
      .propagateSystemProperty("idea.diagnostic.opentelemetry.otlp")
      .propagateSystemProperty("bazel.project.view.file.path")
      .propagateSystemProperty("bazel.enable.log")
      .patchPathVariable()
      .enableCppToolchainDetectionForNestedBazel()
      .filterOutRunfileVariables()
      .applyWindowsEnvironment()
      .addIdeStarterTestMarker()
      .applyVMOptionsPatch {
        addSystemProperty("JETBRAINS_LICENSE_SERVER", "https://flsv1.labs.jb.gg")
        addSystemProperty("idea.trust.disabled", "true")
      }
  }

  @BeforeEach
  fun initialize() {
    di =
      DI {
        extend(di)
        bindSingleton(tag = "teamcity.uri", overrides = true) {
          val teamcityUrl = System.getProperty("bazel.ide.starter.test.teamcity.url") ?: "https://buildserver.labs.intellij.net"
          URI(teamcityUrl).normalize()
        }
        bindSingleton<CIServer>(overrides = true) { TeamCityCIServer() }
        System.getProperty("bazel.ide.starter.test.cache.directory")?.let { root ->
          bindSingleton<GlobalPaths>(overrides = true) {
            object : GlobalPaths(Path.of(root)) {}
          }
        }
      }
  }

  /**
   * A PER_CLASS test keeps one IDE open for all its methods.
   * It stops its processes in [StopOwnedProcessesAfterClass], after the IDE closes.
   */
  @AfterEach
  fun tearDown() {
    if (!ideStaysOpenBetweenTests) stopOwnedProcesses()
  }

  /** Stops the Bazel servers and the JCEF helpers of the contexts of this test, and deletes their Bazel output roots. */
  internal fun stopOwnedProcesses() {
    val outputBases = ownedOutputBases.toSet()
    timeoutRunBlocking(timeout = 1.minutes) {
      val servers = findBazelServers(outputBases)
      HermeticityGuard.assertTeardownOwnsProcesses(outputBases, servers)
      stopProcesses("Bazel server", servers)
      stopProcesses("JCEF helper", findCefProcesses(contexts.map { it.paths.testHome }))
    }
    // A big output base can take longer than the timeout to delete, so this step has no timeout and does not fail the test.
    BazelProjectConfigurer.cleanupNestedBazelRoots(outputBases)
  }

  internal class StopOwnedProcessesAfterClass : AfterAllCallback {
    override fun afterAll(context: ExtensionContext) {
      val test = context.testInstance.orElse(null) as? IdeStarterBaseProjectTest ?: return
      if (test.ideStaysOpenBetweenTests) test.stopOwnedProcesses()
    }
  }

  private fun IDETestContext.propagateSystemProperty(key: String): IDETestContext {
    val value = System.getProperty(key) ?: return this
    applyVMOptionsPatch {
      addSystemProperty(key, value)
    }
    return this
  }

  /**
   * Bazel adds the current version of itself to the PATH variable that is passed to the test.
   * This causes `.bazelversion` of the test project to be ignored.
   */
  private fun IDETestContext.patchPathVariable(): IDETestContext {
    var path = checkNotNull(System.getenv("PATH")) { "PATH is null" }
    val paths = path.split(File.pathSeparator)
    if (paths[0] == "." && "bazelisk" in paths[1]) {
      path = paths.drop(2).joinToString(File.pathSeparator)
    }
    applyVMOptionsPatch {
      withEnv("PATH", path)
      withEnv("HOME", System.getProperty("user.home"))
    }
    return this
  }

  /**
   * Bazelisk 1.30+ uses the nearest `.bazeliskrc` in the project directory or in any parent directory.
   * Test projects are unpacked inside the outer execroot, which links the monorepo `.bazeliskrc`,
   * and its `BAZELISK_BASE_URL` points to the JetBrains Bazel fork.
   * An empty `.bazeliskrc` above the projects stops the lookup there; a project with its own `.bazeliskrc` still uses it.
   */
  private fun hideOuterBazeliskrcFromProjects() {
    val bazeliskrc = GlobalPaths.instance.cacheDirForProjects.createDirectories().resolve(".bazeliskrc")
    if (!bazeliskrc.exists()) bazeliskrc.createFile()
  }

  private fun IDETestContext.enableCppToolchainDetectionForNestedBazel(): IDETestContext {
    applyVMOptionsPatch {
      withEnv("BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN", "0")
      withEnv("BAZEL_NO_APPLE_CPP_TOOLCHAIN", "0")
    }
    return this
  }

  /**
   * IDE-Starter launches fixture Bazel processes from inside an outer Bazel test.
   * On Windows, inherited runfiles variables pointed the nested launcher back into
   * the outer test tree and produced invalid runtime resolution.
   *
   * Strip the outer runfiles environment before starting nested Bazel
   * and cover the retained and removed variables explicitly.
   */
  private fun IDETestContext.filterOutRunfileVariables(): IDETestContext {
    applyVMOptionsPatch {
      withEnv("JAVA_RUNFILES", "")
      withEnv("RUNFILES_DIR", "")
      withEnv("RUNFILES_MANIFEST_FILE", "")
      withEnv("RUNFILES_MANIFEST_ONLY", "")
    }
    return this
  }

  private fun IDETestContext.applyWindowsEnvironment(): IDETestContext {
    if (OS.current == OS.WINDOWS) {
      val tempDir = paths.tempDir.absolutePathString()   // IDEDataPaths.tempDir = <testHome>/temp
      val localAppData =
        System.getenv("LocalAppData")?.takeIf { it.isNotEmpty() }
        ?: Path.of(System.getProperty("user.home"), "AppData", "Local").absolutePathString()

      applyVMOptionsPatch {
        withEnv("BAZEL_SH", "c:\\msys64\\usr\\bin\\bash.exe")
        withEnv("LOCALAPPDATA", localAppData)
        withEnv("USERPROFILE", System.getProperty("user.home"))
        withEnv("TMP", tempDir)
        withEnv("TEMP", tempDir)
      }
    }
    return this
  }

  private fun IDETestContext.addIdeStarterTestMarker(): IDETestContext {
    applyVMOptionsPatch {
      addSystemProperty(IS_IN_IDE_STARTER_TEST, "true")
    }
    return this
  }

  companion object {
    private fun isBazelServer(p: ProcessInfo): Boolean =
      p.arguments.any { arg ->
        arg.contains("A-server.jar") || arg.endsWith("/server.jar") || arg.endsWith("\\server.jar") || arg.endsWith("-server.jar")
      }

    private val isIdeTestsBazelServer = Predicate<ProcessInfo> { p ->
      val fromIdeTestsWorkspace = p.arguments.any { arg ->
        arg.startsWith("--workspace_directory=") && (arg.contains("/ide-tests/") || arg.contains("\\ide-tests\\"))
      }
      isBazelServer(p) && fromIdeTestsWorkspace
    }

    /** Finds the Bazel servers that run with one of the [outputBases]. */
    internal suspend fun findBazelServers(outputBases: Set<String>): List<ProcessInfo> {
      if (outputBases.isEmpty()) return emptyList()
      return getProcessList(
        Predicate { p ->
          isBazelServer(p) && p.arguments.any { arg ->
            arg.startsWith("--output_base=") && HermeticityGuard.normalize(arg.removePrefix("--output_base=")) in outputBases
          }
        },
      )
    }

    /** Finds the JCEF helpers of the IDEs that ran in one of the [testHomes]. */
    private suspend fun findCefProcesses(testHomes: List<Path>): List<ProcessInfo> {
      if (testHomes.isEmpty()) return emptyList()
      val homes = testHomes.map { HermeticityGuard.normalize(it.toAbsolutePath().toString()) }
      return getProcessList(
        Predicate { p ->
          p.name.contains("cef_server") && p.arguments.any { arg -> homes.any { HermeticityGuard.normalize(arg).contains(it) } }
        },
      )
    }

    private suspend fun stopProcesses(description: String, processes: List<ProcessInfo>) {
      try {
        if (processes.isEmpty()) {
          println("Stopping $description processes: no processes were detected")
          return
        }
        println("Stopping $description processes: [${processes.joinToString(", ")}] will be stopped")
        killProcesses(processes)
      } catch (e: CancellationException) {
        throw e
      } catch (t: Throwable) {
        System.err.println("Failed to stop $description processes: ${t.message}")
      }
    }

    suspend fun killBazelProcesses() {
      try {
        // Kill Bazel server Java processes started for the test workspace
        findAndKillProcesses(message = "Killing Bazel server processes", filter = isIdeTestsBazelServer)
      } catch (t: Throwable) {
        System.err.println("Failed to find/kill Bazel server processes: ${t.message}")
      }
    }

    suspend fun killCefProcesses() {
      try {
        val cefProcesses = getProcessList(
          Predicate { p ->
            p.name.contains("cef_server") &&
            p.arguments.any { it.contains("/ide-tests/") || it.contains("\\ide-tests\\") }
          },
        )
        if (cefProcesses.isEmpty()) {
          println("Killing orphaned JCEF helper processes: no processes were detected")
          return
        }
        println("Killing orphaned JCEF helper processes: [${cefProcesses.joinToString(", ")}] will be force-killed")
        cefProcesses.forEach { it.processHandle?.destroyForcibly() }
      } catch (t: Throwable) {
        System.err.println("Failed to find/kill JCEF processes: ${t.message}")
      }
    }
  }
}

/**
 * Builds a CommandChain with [builder] and executes it immediately.
 * Replaces clunky construct like `execute(CommandChain().goto(3, 7))`
 * ```
 */
inline fun Driver.execute(builder: CommandChain.() -> Unit) {
  this.execute(CommandChain().apply(builder))
}

fun Driver.syncBazelProject(buildAndSync: Boolean = false) {
  execute(CommandChain().takeScreenshot("startSync"))
  execute(CommandChain().openBspToolWindow())
  execute(CommandChain().takeScreenshot("openBspToolWindow"))
  execute(CommandChain().waitForBazelSync())
  if (buildAndSync) {
    execute(CommandChain().buildAndSync())
  }
  failIfFatalIdeErrorsPresent("Bazel sync")
  execute(CommandChain().waitForSmartMode())
}

fun Driver.syncBazelProjectCloseDialog(buildAndSync: Boolean = false) {
  execute(CommandChain().takeScreenshot("startSync"))
  execute(CommandChain().openBspToolWindow())
  // close only the Git confirmation dialog; other popups like "Loading file" are not closable via dispose()
  ideFrame {
    val gitAddDialog = dialog(title = "Add File to Git")
    val dialogFound = runCatching { gitAddDialog.waitFound(timeout = 30.seconds) }.isSuccess

    if (dialogFound) {
      runCatching { gitAddDialog.closeDialog() }
        .onFailure { System.err.println("Failed to close 'Add File to Git' dialog: ${it.message}") }
    }
  }
  execute(CommandChain().takeScreenshot("openBspToolWindow"))
  execute(CommandChain().waitForBazelSync())
  if (buildAndSync) {
    execute(CommandChain().buildAndSync())
  }
  failIfFatalIdeErrorsPresent("Bazel sync")
  execute(CommandChain().waitForSmartMode())
}

fun IdeaFrameUI.waitForBazelDebuggerUiReady(
  sessionTimeout: Duration = 3.minutes,
  toolWindowTimeout: Duration = 30.seconds,
  collectingTimeout: Duration = 30.seconds,
): DebugToolWindowUi {
  waitFor(
    message = "Debug session never started: Stop or Restart Debug did not appear in the main toolbar",
    timeout = sessionTimeout,
    interval = 1.seconds,
  ) {
    runCatching {
      mainToolbar.stopButton.present() || mainToolbar.restartDebugButton.present()
    }.getOrDefault(false)
  }

  driver.openToolWindow("Debug")
  waitForIndicators(20.seconds)

  waitFor(
    message = "Debug tool window did not appear",
    timeout = toolWindowTimeout,
    interval = 1.seconds,
  ) {
    runCatching { debugToolWindow().present() }.getOrDefault(false)
  }
  val debugToolWindow = debugToolWindow()

  waitFor(
    message = "Threads & Variables tab did not appear in the Debug tool window",
    timeout = toolWindowTimeout,
    interval = 1.seconds,
  ) {
    runCatching { debugToolWindow.threadsAndVariablesTab.present() }.getOrDefault(false)
  }
  debugToolWindow.threadsAndVariablesTab.click()
  waitForIndicators(20.seconds)

  waitFor(
    message = "Debugger tree did not appear in Threads & Variables",
    timeout = toolWindowTimeout,
    interval = 1.seconds,
  ) {
    runCatching { debugToolWindow.tree().present() }.getOrDefault(false)
  }

  waitFor(
    message = "Debugger tree is still collecting data",
    timeout = collectingTimeout,
    interval = 1.seconds,
  ) {
    runCatching {
      !debugToolWindow.tree().getAllTexts().asString().contains("Collecting", ignoreCase = true)
    }.getOrDefault(false)
  }

  waitFor(
    message = "Resume Program button should be enabled",
    timeout = toolWindowTimeout,
    interval = 1.seconds,
  ) {
    runCatching { debugToolWindow.resumeButton.isEnabled() }.getOrDefault(false)
  }

  return debugToolWindow
}

fun <T : CommandChain> T.openBspToolWindow(): T {
  addCommand(CMD_PREFIX + "openBspToolWindow")
  return this
}

fun <T : CommandChain> T.waitForBazelSync(): T {
  addCommand(CMD_PREFIX + "waitForBazelSync")
  return this
}

fun <T : CommandChain> T.buildAndSync(): T {
  addCommand(CMD_PREFIX + "buildAndSync")
  return this
}

fun <T : CommandChain> T.assertFileContentsEqual(expectedRelativePath: String, actualRelativePath: String): T {
  addCommand(CMD_PREFIX + "assertFileContentsEqual $expectedRelativePath $actualRelativePath")
  return this
}

fun <T : CommandChain> T.assertEitherFileContentIsEqual(actualRelativePath: String, vararg expectedRelativePaths: String): T {
  addCommand(CMD_PREFIX + "assertEitherContentsEqual $actualRelativePath ${expectedRelativePaths.joinToString(" ")}")
  return this
}

fun <T : CommandChain> T.navigateToFile(
  caretLine: Int,
  caretColumn: Int,
  expectedFilename: String,
  expectedCaretLine: Int,
  expectedCaretColumn: Int,
): T =
  this
    .goto(caretLine, caretColumn)
    .delay(500)
    .takeScreenshot("Before navigating to $expectedFilename")
    .goToDeclaration()
    .delay(500)
    .assertCurrentFile(expectedFilename)
    .assertCaretPosition(expectedCaretLine, expectedCaretColumn)

fun IDETestContext.withBazelFeatureFlag(flag: String, value: Boolean = true) = this.applyVMOptionsPatch {
  addSystemProperty(flag, if (value) "true" else "false")
}

fun Driver.singleProjectOrNull(): Project? = service<ProjectManager>(RdTarget.DEFAULT)
  .getOpenProjects()
  .singleOrNull()

fun Driver.failIfFatalIdeErrorsPresent(phase: String) {
  val errors = withContext(OnDispatcher.EDT) {
    utility<IdeMessagePool>().getInstance()
      .getFatalErrors(includeReadMessages = true, includeSubmittedMessages = true)
      .map { IdeErrorText(it.getMessage(), it.getThrowableText()) }
  }
  fatalIdeErrorsFailure(phase, errors)?.let(::error)
}

/**
 * Should be used instead of [com.intellij.driver.sdk.openFile] because this method doesn't rely on content roots
 */
fun Driver.openFile(relativePath: String, waitForCodeAnalysis: Boolean = true): VirtualFile =
  step("Open file $relativePath") {
    val fileToOpen = findFile(relativePath = relativePath) ?: throw IllegalArgumentException("Fail to find file $relativePath")
    openEditor(fileToOpen, singleProject())
    if (waitForCodeAnalysis) {
      waitForCodeAnalysis(singleProject(), fileToOpen)
    }
    fileToOpen
  }

/**
 * Should be used instead of [com.intellij.driver.sdk.findFile] because this method doesn't rely on content roots
 */
fun Driver.findFile(relativePath: String): VirtualFile? = projectRootDir.findFileByRelativePath(relativePath)

val Driver.isBazelProject: Boolean
  get() = service<BazelProjectInfoService>(singleProject()).isBazelProject

val Driver.projectRootDir: VirtualFile
  get() = service<BazelProjectInfoService>(singleProject()).projectRootDir

@Remote("org.jetbrains.bazel.sync.environment.BazelProjectInfoService", plugin = "org.jetbrains.bazel/intellij.bazel.commons")
interface BazelProjectInfoService {
  var isBazelProject: Boolean
  var projectRootDir: VirtualFile
}

@Remote("com.intellij.diagnostic.MessagePool")
interface IdeMessagePool {
  fun getInstance(): IdeMessagePool
  fun getFatalErrors(includeReadMessages: Boolean, includeSubmittedMessages: Boolean): List<IdeFatalError>
}

@Remote("com.intellij.diagnostic.AbstractMessage")
interface IdeFatalError {
  fun getMessage(): String?
  fun getThrowableText(): String
}

fun UiComponent.waitForSyncSucceeded(timeout: Duration = 2.minutes) {
  val buildView = x { byType("com.intellij.build.BuildView") }
  val syncSuccessText = BazelBackendBundle.message("console.task.sync.success")
  buildView.waitOneText(message = "Waiting for Bazel sync to finish", timeout = timeout) {
    it.text.contains(syncSuccessText)
  }
}

fun <T : CommandChain> T.assertSyncedTargets(vararg targets: String): T {
  addCommand(CMD_PREFIX + "assertSyncedTargets ${targets.joinToString(" ")}")
  return this
}

fun <T : CommandChain> T.switchProjectView(fileName: String): T {
  addCommand(CMD_PREFIX + "switchProjectView $fileName")
  return this
}

fun Driver.switchProjectViewWithPreview(fileName: String) {
  execute(CommandChain().switchProjectView(fileName))
  openFile(fileName, waitForCodeAnalysis = false)
  execute(CommandChain().takeScreenshot("projectView_${fileName.substringBeforeLast('.')}"))
}

fun <T : CommandChain> T.refreshFile(relativePath: String): T {
  addCommand(CMD_PREFIX + "refreshFile $relativePath")
  return this
}

fun <T : CommandChain> T.assertFileKind(relativePath: String, vararg expectedFileKind: FileKindCheck): T {
  addCommand(CMD_PREFIX + "assertFileKind $relativePath ${expectedFileKind.joinToString(separator = ",")}")
  return this
}

fun checkIdeaLogForExceptions(context: IDETestContext) {
  val logFile = context.paths.testHome.resolve("log").resolve("idea.log")
  if (!logFile.toFile().exists()) return

  val errors = logFile.toFile().readLines().filter {
    it.contains("ERROR -") || it.contains("SEVERE -")
  }
  if (errors.isNotEmpty()) {
    System.err.println("=== IDEA LOG EXCEPTIONS (${errors.size} found) ===")
    errors.forEach { System.err.println(it) }
    System.err.println("=== END IDEA LOG EXCEPTIONS ===")
  } else {
    println("=== IDEA LOG: no exceptions found ===")
  }
}

fun configureProjectWithHermeticCcToolchain(context: IDETestContext) {
  BazelProjectConfigurer.configureProjectBeforeUse(context)
  BazelProjectConfigurer.addHermeticCcToolchain(context)
}

fun getProjectInfoFromSystemProperties(): ProjectInfoSpec {
  val localProjectPath = System.getProperty("bazel.ide.starter.test.project.path")
  if (localProjectPath != null) {
    return LocalProjectInfo(
      projectDir = Path.of(localProjectPath),
      isReusable = true,
      configureProjectBeforeUse = ::configureProjectWithHermeticCcToolchain,
    )
  }
  val projectUrl = System.getProperty("bazel.ide.starter.test.project.url") ?: "https://github.com/JetBrains/hirschgarten.git"
  val commitHash = System.getProperty("bazel.ide.starter.test.commit.hash").orEmpty()
  val branchName = System.getProperty("bazel.ide.starter.test.branch.name") ?: "252"
  val projectHomeRelativePath: String? = System.getProperty("bazel.ide.starter.test.project.home.relative.path")

  return GitProjectInfo(
    repositoryUrl = projectUrl,
    commitHash = commitHash,
    branchName = branchName,
    projectHomeRelativePath = { if (projectHomeRelativePath != null) it.resolve(projectHomeRelativePath) else it },
    isReusable = true,
    configureProjectBeforeUse = ::configureProjectWithHermeticCcToolchain,
  )
}

fun Driver.bazelClean() {
  step("Run Bazel clean") {
    execute(project = null) { it.runBazelClean() }
  }
  step("VFS refresh") {
    val virtualFileManager = service<VirtualFileManager>()
    virtualFileManager.asyncRefresh()
    ideFrame {
      waitForIndicators()
    }
  }
}

private fun <T : CommandChain> T.runBazelClean(): T {
  addCommand(CMD_PREFIX + "runBazelClean")
  return this
}

fun <T : CommandChain> T.waitForBazelFileEventProcessorIdle(): T {
  addCommand(CMD_PREFIX + "waitForBazelFileEventProcessorIdle")
  return this
}
