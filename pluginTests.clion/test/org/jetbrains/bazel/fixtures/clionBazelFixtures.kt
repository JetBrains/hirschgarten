package org.jetbrains.bazel.fixtures

import com.intellij.clion.testFramework.nolang.junit5.core.LanguageEngine
import com.intellij.clion.testFramework.nolang.junit5.core.withClionTimeout
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.assertLastSyncSucceeded
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.jetbrains.bazel.test.framework.writeProjectView
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val BAZEL_CENTRAL_REGISTRY = URI.create("https://bcr.bazel.build/")

private const val FREEZE_TIMEOUT_PROPERTY = "patch.engine.backend.freeze.timeout"

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
 * The test project can resolve modules from the local test registry, [BazelPathManager.clionTestRegistry],
 * for example the `cc_false_toolchain`.
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
): TestFixture<Project> = testFixture {
  testFixture(debugString = "backendFreezeTimeout") {
    val oldValue = System.setProperty(FREEZE_TIMEOUT_PROPERTY, "-1")
    initialized(Unit) {
      if (oldValue == null) {
        System.clearProperty(FREEZE_TIMEOUT_PROPERTY)
      }
      else {
        System.setProperty(FREEZE_TIMEOUT_PROPERTY, oldValue)
      }
    }
  }.init()

  val projectView = ProjectViewBuilder().addDirectories(".").apply(configure).build()

  val project = setUpClionBazelProject(
    openProject = {
      bazelProjectFixture(
        projectPath,
        buildProject = buildProject,
        bazelVersion = bazelVersion,
        projectsRoot = BazelPathManager.clionTestProjectsRoot,
        jvmToolchains = jvmToolchains,
        registries = listOf(BazelPathManager.clionTestRegistry.toUri(), BAZEL_CENTRAL_REGISTRY),
      ) { writeProjectView(it, projectView) }.init()
    },
    waitForSymbols = { project ->
      assertLastSyncSucceeded(project)

      LOG.info("Waiting for symbols to load")
      LanguageEngine.INSTANCE.waitForSymbols(project)
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
