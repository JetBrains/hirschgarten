package org.jetbrains.bazel.fixtures

import com.intellij.clion.testFramework.nolang.junit5.core.LanguageEngine
import com.intellij.clion.testFramework.nolang.junit5.core.publishThreadDump
import com.intellij.clion.testFramework.nolang.junit5.core.withClionTimeout
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.bazel.runfiles.BazelLabel
import com.intellij.platform.bazel.runfiles.BazelRunfiles
import com.intellij.openapi.vfs.impl.VirtualFilePointerManagerImpl
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.disposableFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import com.intellij.util.io.Decompressor
import kotlinx.coroutines.TimeoutCancellationException
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.BazelRegistrySnapshot
import org.jetbrains.bazel.test.framework.BazelTestCaches.testCacheRoot
import org.jetbrains.bazel.test.framework.assertLastSyncSucceeded
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.jetbrains.bazel.test.framework.writeProjectView
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Opens the Bazel test project at [projectPath], runs a real `performBazelSync`, brings up the CLion
 * Nova (Radler) backend, and returns the ready [Project].
 *
 * The backend is driven only through the [LanguageEngine] abstraction, so this module depends on
 * Radler at **runtime** only; [LanguageEngine.INSTANCE] throws when no engine is on the classpath,
 * making a test using this fixture red exactly when the Radler engine dependency is missing.
 *
 * There is no C++ Bazel aspect yet, so today the synced project has no C++ resolve model and the
 * backend attaches to an empty one — but the structure is ready: point [projectPath] at a C++
 * project and add resolve assertions once the aspect lands.
 *
 * [configure] builds the project view of the test. The fixture writes it before the sync, so the test
 * does not need a project view file in its test data.
 *
 * The test project can resolve modules from the local test registry, [BazelPathManager.testRegistry],
 * for example the `cc_false_toolchain`.
 *
 * The setup fails when the sync reports a warning or an error.
 *
 * For the backend to actually come up, `RESHARPER_HOST_BIN` must point at a built `dotnet/Bin.RiderBackend`.
 */
@TestOnly
internal fun clionBazelProjectFixture(
  projectPath: String,
  bazelVersion: String,
  buildProject: Boolean = false,
  jvmToolchains: Boolean = false,
  configure: ProjectViewBuilder.() -> Unit = {},
): TestFixture<Project> = clionBazelProjectFixture(
  projectsRoot = BazelPathManager.clionTestProjectsRoot,
  projectPath = projectPath,
  bazelVersion = bazelVersion,
  registries = { listOf(BazelPathManager.testRegistry.toUri(), BazelRegistrySnapshot.registry(testCacheRoot()).toUri()) },
  buildProject = buildProject,
  jvmToolchains = jvmToolchains,
  awaitSymbols = true,
  configure = configure,
)

/**
 * Opens the downloaded project in the zip file [archive], with the same setup as [clionBazelProjectFixture].
 *
 * [archive] is the label of the archive, for example `@cc_test_project_cloudflare_workerd//:archive`. The test target must have
 * this label in its `data`.
 *
 * The archive must hold exactly one top-level directory. The fixture unpacks it into a temporary directory and opens
 * this directory. With no [bazelVersion] set, the `.bazelversion` file of the project applies.
 *
 * When [waitForSymbols] is off, the fixture does not wait until the backend has loaded the symbols. The backend
 * continues to index while the test runs. Turn it off for a test that reads only the project model, because the
 * symbols of a large project can take minutes to load.
 */
@TestOnly
internal fun clionBazelArchiveProjectFixture(
  archive: String,
  bazelVersion: String? = null,
  buildProject: Boolean = false,
  waitForSymbols: Boolean = true,
  configure: ProjectViewBuilder.() -> Unit = {},
): TestFixture<Project> = testFixture(debugString = "clionBazelArchiveProject") {

  // turn off the consistency check of the virtual file pointers, since it can take minutes for the many source files of a large project
  VirtualFilePointerManagerImpl.disableConsistencyChecksInTestsTemporarily(disposableFixture().init())

  val projectsRoot = tempPathFixture().init()
  val archiveFile = BazelRunfiles.getFileByLabel(BazelLabel.fromString(archive))

  LOG.info("Unpacking the project archive $archive into $projectsRoot")
  Decompressor.Zip(archiveFile).withZipExtensions().extract(projectsRoot)

  val projectDir = projectsRoot.listDirectoryEntries().singleOrNull()?.takeIf { it.isDirectory() }
                   ?: error("The project archive $archive must hold exactly one top-level directory")

  val project = clionBazelProjectFixture(
    projectsRoot = projectsRoot,
    projectPath = projectDir.name,
    bazelVersion = bazelVersion,
    registries = { emptyList() },
    buildProject = buildProject,
    jvmToolchains = false,
    awaitSymbols = waitForSymbols,
    configure = configure,
  ).init()

  initialized(project) {}
}

private fun clionBazelProjectFixture(
  projectsRoot: Path,
  projectPath: String,
  bazelVersion: String?,
  registries: () -> List<URI>,
  buildProject: Boolean,
  jvmToolchains: Boolean,
  awaitSymbols: Boolean,
  configure: ProjectViewBuilder.() -> Unit,
): TestFixture<Project> = testFixture {
  val projectView = ProjectViewBuilder().addDirectories(".").apply(configure).build()

  val project = setUpClionBazelProject(
    openProject = {
      bazelProjectFixture(
        projectPath,
        buildProject = buildProject,
        bazelVersion = bazelVersion,
        projectsRoot = projectsRoot,
        jvmToolchains = jvmToolchains,
        registries = registries(),
      ) { writeProjectView(it, projectView) }.init()
    },
    waitForSymbols = { project ->
      assertLastSyncSucceeded(project, allowWarnings = false)
      if (!awaitSymbols) return@setUpClionBazelProject

      LOG.info("Waiting for symbols to load")
      try {
        LanguageEngine.INSTANCE.waitForSymbols(project)
      }
      catch (e: TimeoutCancellationException) {
        // The index wait has its own timeout, which fires before the setup timeout and publishes no coroutine dump.
        publishThreadDump("indexingStall")
        throw e
      }
    },
  )

  LOG.info("The CLion Bazel project fixture for $projectPath is ready")
  initialized(project) {}
}

/**
 * Runs the steps of the fixture setup that wait for the sync or for the backend: [openProject], which includes the sync,
 * and [waitForSymbols]. Put every new step that can wait in one of them.
 *
 * The steps run one after the other, under one [timeout]. A hang fails the test with a thread dump.
 */
internal suspend fun <T> setUpClionBazelProject(
  timeout: Duration = CLION_BAZEL_SETUP_TIMEOUT,
  openProject: suspend () -> T,
  waitForSymbols: suspend (T) -> Unit,
): T = withClionTimeout(timeout) {
  val project = openProject()
  waitForSymbols(project)
  project
}

private val CLION_BAZEL_SETUP_TIMEOUT = 20.minutes

private val LOG = fileLogger()
