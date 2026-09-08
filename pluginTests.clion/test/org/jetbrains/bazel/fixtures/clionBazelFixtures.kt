package org.jetbrains.bazel.fixtures

import com.intellij.clion.testFramework.nolang.junit5.core.LanguageEngine
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.jetbrains.bazel.test.framework.writeProjectView

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
 * For the backend to actually come up, `RESHARPER_HOST_BIN` must point at a built `dotnet/Bin.RiderBackend`.
 */
@TestOnly
internal fun clionBazelProjectFixture(
  projectPath: String,
  bazelVersion: String? = null,
  buildProject: Boolean = false,
  jvmToolchains: Boolean = false,
  configure: ProjectViewBuilder.() -> Unit = {},
): TestFixture<Project> = testFixture {
  System.setProperty("patch.engine.backend.freeze.timeout", "-1")

  val projectView = ProjectViewBuilder().addDirectories(".").apply(configure).build()

  val project = bazelProjectFixture(
    projectPath,
    buildProject = buildProject,
    bazelVersion = bazelVersion,
    projectsRoot = BazelPathManager.clionTestProjectsRoot,
    jvmToolchains = jvmToolchains,
  ) { writeProjectView(it, projectView) }.init()

  LOG.info("Calling after project opened (engine)")
  LanguageEngine.INSTANCE.afterProjectOpened(project)

  LOG.info("Waiting for symbols to load")
  LanguageEngine.INSTANCE.waitForSymbols(project)

  LOG.info("The CLion Bazel project fixture for $projectPath is ready")
  initialized(project) {}
}

private val LOG = fileLogger()
