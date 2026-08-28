package org.jetbrains.bazel.test.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * This fixture provides necessary functionality to perform a full Bazel sync in tests without a full IDE.
 *
 * It's important to use [copyBazelTestProject] because it provides correct setup of Bazel caches.
 * Not using it might not necessarily break the sync, but it will lead to VFS root access errors.
 *
 * Prefer the declarative [bazelProjectFixture] or [bazelSyncCodeInsightFixture] entry points. They copy
 * and sync the project during the fixture setup, so a test does not repeat that boilerplate.
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

/**
 * Composes a [BazelSyncCodeInsightTestFixture] from an existing [projectFixture] and [tempDirFixture].
 *
 * Use this only when a test must own the project and the temp dir fixtures. Otherwise prefer the
 * declarative [bazelProjectFixture] or [bazelSyncCodeInsightFixture] with a project path.
 */
fun bazelSyncCodeInsightFixture(
  projectFixture: TestFixture<Project>,
  tempDirFixture: TestFixture<Path>,
) = codeInsightFixture(projectFixture, tempDirFixture, ::BazelSyncCodeInsightTestFixtureImpl)

/**
 * Copies the project at [projectPath], syncs it, and returns the ready [Project].
 *
 * The copy and the sync run during the fixture setup, without a code-insight fixture. [configure] runs
 * after the copy and before the sync, for project-level setup.
 *
 * Use this when a test needs only the [Project]. Use [bazelSyncCodeInsightFixture] when the test also
 * needs code insight, for example [checkHighlighting].
 */
@TestOnly
fun bazelProjectFixture(
  projectPath: String,
  buildProject: Boolean = false,
  bazelVersion: String? = null,
  projectView: String? = null,
  projectsRoot: Path = BazelPathManager.testProjectsRoot,
  configure: suspend (Project) -> Unit = {},
): TestFixture<Project> = testFixture(debugString = "bazelProject") {
  val project = projectFixture(openAfterCreation = true).init()
  val projectRoot = tempPathFixture().init()

  val setupDisposable = Disposer.newDisposable("bazelProjectFixture")
  installTestConsoleService(project, setupDisposable)
  initializeBazelProject(project, projectRoot)

  BazelTestProject.copy(project, projectRoot, projectPath, projectsRoot)
  if (bazelVersion != null) {
    writeBazelVersion(projectRoot, bazelVersion)
  }
  if (projectView != null) {
    applyProjectView(project, projectRoot, projectView)
  }
  configure(project)
  runBazelSync(project, buildProject)

  initialized(project) {
    // Stop the bazel server first, so it releases the file locks before the temp dir is removed.
    stopBazelServer(project, projectRoot)
    purgeProjectJdkTable()
    Disposer.dispose(setupDisposable)
  }
}

/**
 * Copies the project at [projectPath], syncs it, and returns the ready [BazelSyncCodeInsightTestFixture].
 *
 * The copy and the sync run during the fixture setup. [configure] runs after the copy and before the
 * sync. Use it for setup that a sync needs, for example [enableGoHighlighting].
 *
 * Use this when a test needs code insight, for example [checkHighlighting]. Use [bazelProjectFixture]
 * when the test needs only the [Project].
 */
@TestOnly
fun bazelSyncCodeInsightFixture(
  projectPath: String,
  buildProject: Boolean = false,
  bazelVersion: String? = null,
  projectView: String? = null,
  configure: suspend (BazelSyncCodeInsightTestFixture) -> Unit = {},
): TestFixture<BazelSyncCodeInsightTestFixture> = testFixture(debugString = "bazelSyncCodeInsight") {
  val fixture = bazelSyncCodeInsightFixture(projectFixture(openAfterCreation = true), tempPathFixture()).init()
  fixture.syncBazelTestProject(projectPath, buildProject, bazelVersion, projectView, configure)
  initialized(fixture) {}
}

private suspend fun BazelSyncCodeInsightTestFixture.syncBazelTestProject(
  projectPath: String,
  buildProject: Boolean,
  bazelVersion: String?,
  projectView: String?,
  configure: suspend (BazelSyncCodeInsightTestFixture) -> Unit,
) {
  copyBazelTestProject(projectPath)
  if (bazelVersion != null) {
    setBazelVersion(bazelVersion)
  }
  if (projectView != null) {
    setProjectView(projectView)
  }
  configure(this)
  performBazelSync(buildProject)
}

class BazelSyncCodeInsightTestFixtureImpl(
  projectFixture: IdeaProjectTestFixture,
  tempDirTestFixture: TempDirTestFixture,
) : CodeInsightTestFixtureImpl(projectFixture, tempDirTestFixture), BazelSyncCodeInsightTestFixture {
  private val projectRoot: Path
    get() = Path(tempDirPath)

  init {
    installTestConsoleService(project, testRootDisposable)
  }

  override fun copyBazelTestProject(path: String) {
    BazelTestProject.copy(project, projectRoot, path)
  }

  override fun setProjectView(projectview: String) {
    applyProjectView(project, projectRoot, projectview)
  }

  override fun setBazelVersion(version: String) {
    writeBazelVersion(projectRoot, version)
  }

  override suspend fun performBazelSync(buildProject: Boolean) {
    runBazelSync(project, buildProject)
  }

  override fun setUp() {
    super.setUp()
    initializeBazelProject(project, projectRoot)
  }

  override fun tearDown() {
    try {
      // Stop bazel server to unlock access to all files
      runBlocking(Dispatchers.Default) {
        stopBazelServer(project, projectRoot)
      }
      purgeProjectJdkTable()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}
