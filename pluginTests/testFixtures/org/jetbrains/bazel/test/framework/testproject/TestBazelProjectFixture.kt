package org.jetbrains.bazel.test.framework.testproject

import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.disposableFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelOutputBaseFixture
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Create fake bazel project, which resemble real bazel project state after sync
 *
 * The workspace root is the project directory, or its [workspace] subdirectory.
 * The install writes an empty `MODULE.bazel` file in the workspace root when the file is not there.
 * The install publishes an empty synced snapshot with the fixture BazelInfo.
 * The fixture adds no project directories. Call [projectDirectories] to add them.
 *
 * Declare the result as an instance property of a class with [BazelTestApplication], so that each test gets a new project.
 * A property in a companion object shares one project between all tests of the class.
 *
 * Use [org.jetbrains.bazel.test.framework.bazelCodeInsightFixture] when full code insight fixture is needed
 * Use [org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture] when test is going to interact with real bazel process
 *
 * @param openAfterCreation Set it to false to keep the project closed. A closed project runs no startup activity
 * @param workspace Workspace root, relative to the project directory, fixture creates that directory
 * @param persistentSnapshots Set it to true to keep the snapshots in the persistent store.
 * @param beforeInstall Runs on a background thread after the fixture creates the directories and before the install.
 */
fun testBazelProjectFixture(
  openAfterCreation: Boolean = true,
  workspace: String = "",
  persistentSnapshots: Boolean = false,
  beforeInstall: (workspaceRoot: Path, outputBase: Path) -> Unit = { _, _ -> },
): TestFixture<TestBazelProject> =
  projectFixture(openAfterCreation = openAfterCreation)
    .testBazelProjectFixture(workspace = workspace, persistentSnapshots = persistentSnapshots, beforeInstall = beforeInstall)

internal fun TestFixture<Project>.testBazelProjectFixture(
  rootFixture: TestFixture<Path>? = null,
  realBazel: Boolean = false,
  workspace: String = "",
  persistentSnapshots: Boolean = false,
  beforeInstall: (workspaceRoot: Path, outputBase: Path) -> Unit = { _, _ -> },
): TestFixture<TestBazelProject> = testFixture("testBazelProject") {
  val project = this@testBazelProjectFixture.init()
  val root = (rootFixture?.init() ?: project.stateStore.projectBasePath).resolve(workspace).createDirectories()
  val outputBase = bazelOutputBaseFixture().init()
  val parentDisposable = disposableFixture().init()
  val testBazel = withContext(Dispatchers.IO) {
    beforeInstall(root, outputBase)
    project.installTestBazelProject(root, outputBase, parentDisposable, realBazel, persistentSnapshots)
  }
  initialized(testBazel) {
    withContext(Dispatchers.IO) { testBazel.uninstall() }
  }
}

internal fun TestFixture<TestBazelProject>.installedProjectFixture(): TestFixture<Project> =
  testFixture("installedProject") {
    initialized(this@installedProjectFixture.init().project) {}
  }

internal fun TestFixture<TestBazelProject>.workspaceRootFixture(): TestFixture<Path> =
  testFixture("testBazelWorkspaceRoot") {
    initialized(this@workspaceRootFixture.init().workspaceRoot) {}
  }
