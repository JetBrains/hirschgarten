package org.jetbrains.bazel.fixtures

import com.intellij.clion.testFramework.nolang.junit5.core.LanguageEngine
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightTestFixture
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture

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
 * For the backend to actually come up, `RESHARPER_HOST_BIN` must point at a built `dotnet/Bin.RiderBackend`.
 */
@TestOnly
internal fun clionBazelProjectFixture(
  projectPath: String,
  projectView: String? = null,
  bazelVersion: String? = null,
  buildProject: Boolean = false,
  configure: suspend (BazelSyncCodeInsightTestFixture) -> Unit = {},
): TestFixture<Project> = testFixture {

  System.setProperty("patch.engine.backend.freeze.timeout", "-1")

  val fixture = bazelSyncCodeInsightFixture(projectFixture(openAfterCreation = true), tempPathFixture()).init()
  fixture.copyBazelTestProject(projectPath)
  if (bazelVersion != null) {
    fixture.setBazelVersion(bazelVersion)
  }
  if (projectView != null) {
    fixture.setProjectView(projectView)
  }
  configure(fixture)
  fixture.performBazelSync(buildProject)

  val project = fixture.project
  LanguageEngine.INSTANCE.afterProjectOpened(project)
  LanguageEngine.INSTANCE.waitForSymbols(project)

  initialized(project) {}
}
