package org.jetbrains.bazel.test.framework

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.nio.file.FileVisitResult
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.pathString
import kotlin.io.path.setAttribute
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.visitFileTree

interface BazelSyncCodeInsightTestFixture : CodeInsightTestFixture {

  fun copyBazelTestProject(path: String, vararg variables: Pair<String, String>)

  suspend fun performBazelSync()
}

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

  init {
    testDataPath = BazelPathManager.testProjectsRoot.pathString
  }

  override fun copyBazelTestProject(path: String, vararg variables: Pair<String, String>) {
    copyDirectoryToProject("base", "")
    copyDirectoryToProject(path, "")
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
