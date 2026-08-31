package org.jetbrains.bazel.test.framework

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FailureResult
import com.intellij.build.events.FinishEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.OutputBuildEvent
import com.intellij.configurationStore.ProjectStoreImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.util.Disposer
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.project.stateStore
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.replaceService
import com.intellij.util.lang.UrlClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.sync.BazelEnvironmentService
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.ui.console.task.TestTaskConsole
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.kotlin.incremental.createDirectory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath
import kotlin.io.path.writeText

/**
 * This fixture provides necessary functionality to perform a full Bazel sync in tests without a full IDE.
 *
 * It's important to use [copyBazelTestProject] because it provides correct setup of Bazel caches.
 * Not using it might not necessarily break the sync, but it will lead to VFS root access errors.
 */
interface BazelSyncCodeInsightTestFixture : CodeInsightTestFixture {

  /**
   * Copies the test project to [tempDirPath] from the [path] relative to [BazelPathManager.testProjectsRoot].
   * It **does not** overwrite [testDataPath].
   *
   * Before coping your test project, it also copies testProjects/base.
   * If any file from your project collides with the base, your file will overwrite the base file.
   */
  fun copyBazelTestProject(path: String)

  fun setProjectView(projectview: String)

  fun setBazelVersion(version: String)

  suspend fun performBazelSync(buildProject: Boolean = false)
}

fun bazelSyncCodeInsightFixture(
  projectFixture: TestFixture<Project>,
  tempDirFixture: TestFixture<Path>,
) = codeInsightFixture(projectFixture, tempDirFixture, ::BazelSyncCodeInsightTestFixtureImpl)

class BazelSyncCodeInsightTestFixtureImpl(
  projectFixture: IdeaProjectTestFixture,
  tempDirTestFixture: TempDirTestFixture,
) : CodeInsightTestFixtureImpl(projectFixture, tempDirTestFixture), BazelSyncCodeInsightTestFixture {
  private companion object {
    const val BAZEL_SETTINGS_START = "# BEGIN IntelliJ Bazel unit-test settings"
    const val BAZEL_SETTINGS_END = "# END IntelliJ Bazel unit-test settings"
  }

  private var testProjectPath: Path? = null

  private val projectRoot: Path
    get() = Path(tempDirPath)

  init {
    project.replaceService(
      ConsoleService::class.java,
      TestConsoleService(project).also { Disposer.register(testRootDisposable, it) },
      testRootDisposable,
    )
  }

  override fun copyBazelTestProject(path: String) {
    val testProjectsPath = BazelPathManager.testProjectsRoot.relativeTo(Path(testDataPath))
    copyDirectoryToProject("${testProjectsPath}/base", "")
    setupBazelRc()
    copyDirectoryToProject("${testProjectsPath}/$path", "")
    configureBazelCaches(path)
    findKotlinStdlibInClasspath().copyTo(projectRoot.resolve("toolchains").resolve("kotlin-stdlib.jar").createParentDirectories())
    testProjectPath = BazelPathManager.testProjectsRoot.resolve(path)
  }

  // create %user_home%/bazel-test-temp
  private fun setupBazelRc() {
    val bazelCachesPath: String = run {
      val testCaches = File(System.getProperty("user.home"), "bazel-test-temp")
      testCaches.createDirectory()
      serializeBazelRcPath(testCaches.absolutePath)
    }

    File(tempDirPath, ".bazelrc")
      .writeText(
        """
        startup --host_jvm_args=-Djava.io.tmpdir=$bazelCachesPath

        common --action_env BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN=0
        common --action_env BAZEL_NO_APPLE_CPP_TOOLCHAIN=0

        build --java_runtime_version=remotejdk_21

        build --action_env=TMP=$bazelCachesPath
        build --action_env=TEMP=$bazelCachesPath
        """.trimIndent()
    )
  }

  override fun setProjectView(projectview: String) {
    val source = projectRoot.resolve(projectview).takeIf { it.exists() } ?: return
    val descriptor = (project.stateStore as ProjectStoreImpl).storeDescriptor as BazelProjectStoreDescriptor
    source.copyTo(descriptor.projectViewFile.createParentDirectories(), overwrite = true)
  }

  override fun setBazelVersion(version: String) {
    projectRoot.resolve(".bazelversion").writeText(version)
  }

  private fun configureBazelCaches(testProjectPath: String) {
    val cacheRoot = testCacheRoot()
      .resolve(cacheGroup(testProjectPath))
      .createDirectories()

    val bazeliskCache = cacheRoot.resolve("bazelisk").createDirectories()
    projectRoot.resolve(".bazeliskrc").writeText("BAZELISK_HOME=${bazeliskCache.toBazelPath()}\n")

    val repositoryCache = cacheRoot.resolve("repository-cache").createDirectories()
    val diskCache = cacheRoot.resolve("disk-cache").createDirectories()
    val outputUserRoot = cacheRoot.resolve("output-user-root").createDirectories()
    val outputBase = cacheRoot.resolve("output-bases").resolve(cacheKey(testProjectPath)).createDirectories()
    val lines = listOf(
      "startup --max_idle_secs=${bazelServerMaxIdleSeconds()}",
      "startup --output_user_root=${outputUserRoot.toBazelRcPath()}",
      "startup --output_base=${outputBase.toBazelRcPath()}",
      "common --repository_cache=${repositoryCache.toBazelRcPath()}",
      "common --disk_cache=${diskCache.toBazelRcPath()}",
    )
    writeManagedBazelrcBlock(projectRoot.resolve(".bazelrc"), lines)
  }

  private fun bazelServerMaxIdleSeconds(): Int =
    System.getenv("BAZEL_PLUGIN_TEST_BAZEL_MAX_IDLE_SECONDS")
      ?.toIntOrNull()
    ?: System.getProperty("bazel.plugin.test.bazel.max.idle.seconds")
      ?.toIntOrNull()
    ?: 7200

  private fun testCacheRoot(): Path {
    val cacheRoot = System.getenv("BAZEL_PLUGIN_TEST_CACHE_ROOT")
                      ?.let { Path.of(it) }
                    ?: System.getProperty("bazel.plugin.test.cache.root")
                      ?.let { Path.of(it) }
                    ?: System.getProperty("agent.persistent.cache")
                      ?.takeIf { it.isNotBlank() }
                      ?.let { Path.of(it, "bazel-plugin-test-cache") }
                    ?: System.getenv("AGENT_PERSISTENT_CACHE")
                      ?.takeIf { it.isNotBlank() }
                      ?.let { Path.of(it, "bazel-plugin-test-cache") }
                    ?: localDefaultCacheRoot()
    return cacheRoot.toAbsolutePath()
  }

  private fun cacheGroup(testProjectPath: String): String =
    if (testProjectPath.startsWith("redcodes/")) "redcodes" else cacheKey(testProjectPath)

  private fun cacheKey(testProjectPath: String): String =
    testProjectPath.replace('/', '_').replace('\\', '_')

  private fun localDefaultCacheRoot(): Path {
    val userHome = Path.of(System.getProperty("user.home"))
    val osName = System.getProperty("os.name")
    return when {
      osName.startsWith("Mac", ignoreCase = true) ->
        userHome.resolve("Library").resolve("Caches").resolve("JetBrains").resolve("bazel-plugin-tests")

      osName.startsWith("Windows", ignoreCase = true) -> {
        val localAppData = System.getenv("LOCALAPPDATA")
                             ?.takeIf { it.isNotBlank() }
                             ?.let { Path.of(it) }
                           ?: userHome.resolve("AppData").resolve("Local")
        localAppData.resolve("JetBrains").resolve("bazel-plugin-tests")
      }

      else -> {
        val cacheHome = System.getenv("XDG_CACHE_HOME")
                          ?.takeIf { it.isNotBlank() }
                          ?.let { Path.of(it) }
                        ?: userHome.resolve(".cache")
        cacheHome.resolve("JetBrains").resolve("bazel-plugin-tests")
      }
    }
  }

  private fun writeManagedBazelrcBlock(bazelrc: Path, lines: List<String>) {
    val existingContent = if (bazelrc.exists()) bazelrc.toFile().readText() else ""
    val managedBlockPattern =
      Regex("""(?s)\n?\Q$BAZEL_SETTINGS_START\E.*?\Q$BAZEL_SETTINGS_END\E\n?""")
    val baseContent = existingContent.replace(managedBlockPattern, "\n").trimEnd()
    val managedBlock = buildString {
      appendLine(BAZEL_SETTINGS_START)
      lines.forEach(::appendLine)
      appendLine(BAZEL_SETTINGS_END)
    }
    val separator = if (baseContent.isBlank()) "" else "\n\n"
    bazelrc.writeText(baseContent + separator + managedBlock)
  }

  private fun Path.toBazelPath(): String =
    toAbsolutePath().toString().replace('\\', '/')

  private fun findKotlinStdlibInClasspath(): Path {
    val urls = (this::class.java.classLoader as UrlClassLoader).urls
    return urls.map { it.toURI().toPath() }.first { it.name.startsWith("kotlin-stdlib") }
  }

  override suspend fun performBazelSync(buildProject: Boolean) {
    project.service<ProjectSyncService>().sync(ProjectSyncScope.Full(build = buildProject, phased = false))
  }

  override fun setUp() {
    super.setUp()
    initializeBazelProject(project, projectRoot)
  }

  override fun tearDown() {
    try {
      // Stop bazel server to unlock access to all files
      runBlocking(Dispatchers.Default) {
        shutdownBazelSever()
      }

      //
      WriteAction.runAndWait<Throwable> {
        ProjectJdkTable.getInstance().apply {
          allJdks.forEach(this::removeJdk)
        }
      }
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  private suspend fun shutdownBazelSever(): BazelProcessResult {
    val bazelProcessLauncher =
      BazelProcessLauncherProvider.getInstance()
        .createBazelProcessLauncher(
          projectRoot,
          BazelEnvironmentService.getInstance(project).getEnvironment(),
        )
    val projectView = ProjectViewService.getInstance(project).projectView
    val bazelRunner = BazelRunner.create(
      project, null, projectRoot, bazelProcessLauncher,
      projectView,
    )
    return bazelRunner.run {
      val command =
        buildBazelCommand(projectView) {
          shutDown()
        }
      runBazelCommand(command, TaskGroupId.EMPTY.task(""))
        .waitAndGetResult()
    }
  }
}

private class TestConsoleService(project: Project) : ConsoleService, Disposable {
  override val buildConsole: TaskConsole
  override val syncConsole: TaskConsole

  private val log = logger<TestConsoleService>()

  private fun String?.trimCrLf(): String? =
    this?.trimEnd { it.isWhitespace() || it == '\r' || it == '\n' }

  private fun onEventImpl(buildId: Any, event: BuildEvent) {
    if (event is FinishEvent && event.result is FailureResult) {
      val failure = event.result as FailureResult
      log.error(
        "Bazel build finished with error: ${event.message.trimCrLf()} " +
        failure.failures.joinToString(";") { f ->
          buildString {
            if (f.message != null) append(f.message.trimCrLf())
            if (f.description != null) append(" (").append(f.message.trimCrLf()).append(")")
            if (f.error != null) appendLine().append(f.error).appendLine()
          }
        },
        failure.failures.firstOrNull()?.error,
      )
    }
    if (event is MessageEvent) {
      when (event.kind) {
        MessageEvent.Kind.ERROR -> log.warn("Bazel build error: ${event.message.trimCrLf()}")
        MessageEvent.Kind.WARNING -> log.warn("Bazel build warning: ${event.message.trimCrLf()}")
        else -> log.warn("Bazel build message: ${event.message.trimCrLf()}")
      }
    }
    if (event is OutputBuildEvent && event.parentId == null) {
      log.info("Bazel build message: ${event.message.trimCrLf()}")
    }
  }

  override fun dispose() {}

  init {
    buildConsole = TestTaskConsole(
      object : BuildViewManager(project) {
        override fun onEvent(buildId: Any, event: BuildEvent) {
          onEventImpl(buildId, event)
          super.onEvent(buildId, event)
        }
      }.also { Disposer.register(this, it) },
      "", project,
    )

    syncConsole = TestTaskConsole(
      object : SyncViewManager(project) {
        override fun onEvent(buildId: Any, event: BuildEvent) {
          onEventImpl(buildId, event)
          super.onEvent(buildId, event)
        }
      }.also { Disposer.register(this, it) },
      "", project,
    )
  }
}
