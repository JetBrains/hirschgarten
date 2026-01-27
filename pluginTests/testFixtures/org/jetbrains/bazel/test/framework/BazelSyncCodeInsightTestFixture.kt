package org.jetbrains.bazel.test.framework

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.junit5.fixture.TestFixture
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.nio.file.FileVisitResult
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.io.path.setAttribute
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.visitFileTree

/**
 * This fixture provides necessary functionality to perform a full Bazel sync in tests without a full IDE.
 *
 * It's important to use [copyBazelTestProject] because it provides correct setup of Bazel caches.
 * Not using it might not necessarily break the sync, but it will lead to VFS root access errors.
 */
interface BazelSyncCodeInsightTestFixture : CodeInsightTestFixture {

  /**
   * Copies the test project to [tempDirPath] from the [path] relative to [BazelPathManager.testProjectsRoot] and
   * evaluates Velocity templates with [variables]. It **does not** overwrite [testDataPath].
   *
   * Before coping your test project, it also copies testProjects/base.
   * If any file from your project collides with the base, your file will overwrite the base file.
   * If you want to overwrite .bazelrc, it's important to do it by copying testProjects/base/.bazelrc.tempalate and adding your changes below.
   * This file provides correct Bazel caches setup, which is necessary to avoid VFS root access errors.
   */
  fun copyBazelTestProject(path: String, vararg variables: Pair<String, String>)

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

  private val cacheDir by lazy { createTempDirectory().toRealPath() }
  private val cacheDirVariables by lazy {
    listOf(
      "outputUserRoot" to cacheDir.resolve("output-user-root").createDirectories().pathString,
      "diskCache" to cacheDir.resolve("disk-cache").createDirectories().pathString,
      "repoCache" to cacheDir.resolve("repo-cache").createDirectories().pathString,
    )
  }

  override fun copyBazelTestProject(path: String, vararg variables: Pair<String, String>) {
    val testProjectsPath = BazelPathManager.testProjectsRoot.relativeTo(Path(testDataPath))
    copyDirectoryToProject("${testProjectsPath}/base", "")
    copyDirectoryToProject("${testProjectsPath}/$path", "")
    materializeTemplateFilesInProject(cacheDirVariables.toMap() + variables.toMap())
  }

  override suspend fun performBazelSync() {
    LoggedErrorProcessor
      .executeWith(VfsRootAccessErrorProcessor)
      .use {
        ProjectSyncTask(project).sync(SecondPhaseSync, true)
      }
  }

  override fun setUp() {
    super.setUp()
    project.bazelProjectProperties.rootDir = virtualFileOf(tempDirPath)
    VfsRootAccess.allowRootAccess(project, cacheDir.pathString)
  }

  override fun tearDown() {
    try {
      project.bazelProjectProperties.rootDir = null
      WriteAction.runAndWait<Throwable> {
        ProjectJdkTable.getInstance().apply {
          allJdks.forEach(this::removeJdk)
        }
      }
      //TODO: BAZEL-2865 get read of deleteRecursivelyWithPermissionFix and replace with something sensible
      deleteRecursivelyWithPermissionFix(cacheDir)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}

private fun deleteRecursivelyWithPermissionFix(path: Path) = path.visitFileTree {
  onPreVisitDirectory { directory, _ ->
    directory.makeWritable()
    FileVisitResult.CONTINUE
  }

  onVisitFile { file, _ ->
    file.makeWritable()
    file.deleteIfExists()
    FileVisitResult.CONTINUE
  }

  onVisitFileFailed { file, _ ->
    file.deleteIfExists()
    FileVisitResult.CONTINUE
  }

  onPostVisitDirectory { directory, _ ->
    directory.deleteIfExists()
    FileVisitResult.CONTINUE
  }
}

private fun Path.makeWritable() {
  try {
    val permissions = getPosixFilePermissions(LinkOption.NOFOLLOW_LINKS)
    if (PosixFilePermission.OWNER_WRITE !in permissions) {
      setPosixFilePermissions(permissions + PosixFilePermission.OWNER_WRITE)
    }
  }
  catch (_: UnsupportedOperationException) {
    // Not POSIX (Windows) - remove read-only attribute
    runCatching { setAttribute("dos:readonly", false, LinkOption.NOFOLLOW_LINKS) }
  }
}

private object VfsRootAccessErrorProcessor : LoggedErrorProcessor() {
  override fun processError(
    category: String,
    message: String,
    details: Array<out String>,
    t: Throwable?,
  ): Set<Action> {
    if (t is VfsRootAccess.VfsRootAccessNotAllowedError) {
      return setOf(Action.LOG)
    }
    return super.processError(category, message, details, t)
  }
}
