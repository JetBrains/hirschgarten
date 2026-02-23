package org.jetbrains.bazel.test.framework

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.util.lang.UrlClassLoader
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath

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

  suspend fun performBazelSync()
}

fun bazelSyncCodeInsightFixture(
  projectFixture: TestFixture<Project>,
  tempDirFixture: TestFixture<Path>,
) = codeInsightFixture(projectFixture, tempDirFixture, ::BazelSyncCodeInsightTestFixtureImpl)

class BazelSyncCodeInsightTestFixtureImpl(
  projectFixture: IdeaProjectTestFixture,
  tempDirTestFixture: TempDirTestFixture,
) : CodeInsightTestFixtureImpl(projectFixture, tempDirTestFixture), BazelSyncCodeInsightTestFixture {

  private var testProjectPath: Path? = null

  private val tempDir: Path
    get() = Path(tempDirPath)

  override fun copyBazelTestProject(path: String) {
    val testProjectsPath = BazelPathManager.testProjectsRoot.relativeTo(Path(testDataPath))
    copyDirectoryToProject("${testProjectsPath}/base", "")
    copyDirectoryToProject("${testProjectsPath}/$path", "")
    findKotlinStdlibInClasspath().copyTo(tempDir.resolve("toolchains").resolve("kotlin-stdlib.jar").createParentDirectories())
    testProjectPath = BazelPathManager.testProjectsRoot.resolve(path)
  }

  private fun findKotlinStdlibInClasspath(): Path {
    val urls = (this::class.java.classLoader as UrlClassLoader).urls
    return urls.map { it.toURI().toPath() }.first { it.name.startsWith("kotlin-stdlib") }
  }

  override suspend fun performBazelSync() {
    ProjectSyncTask(project).sync(SecondPhaseSync, buildProject = false)
  }

  override fun setUp() {
    super.setUp()
    project.bazelProjectProperties.rootDir = virtualFileOf(tempDirPath)
  }

  override fun tearDown() {
    try {
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
}
