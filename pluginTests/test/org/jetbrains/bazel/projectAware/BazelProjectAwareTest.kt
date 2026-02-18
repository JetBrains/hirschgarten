package org.jetbrains.bazel.projectAware

import com.intellij.openapi.vfs.VirtualFile
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class BazelProjectAwareTest : WorkspaceModelBaseTest() {
  @Test
  fun `get top-level setting files`() {
    prepareFiles()
    val projectAware = BazelProjectAware(project)

    val result = shouldNotThrowAny { projectAware.settingsFiles }

    val fileNames = result.map { Path.of(it).fileName.toString() }
    fileNames shouldContainAll listOf("MODULE.bazel", ".bazelrc")
  }

  @Test
  fun `should cache results`() {
    prepareFiles()
    val projectAware = BazelProjectAware(project)
    val syncCache = SyncCache.getInstance(project)

    syncCache.isAlreadyComputed(projectAware.cachedBazelFiles).shouldBeFalse()
    projectAware.settingsFiles
    syncCache.isAlreadyComputed(projectAware.cachedBazelFiles).shouldBeTrue()
  }

  @Test
  fun `should get cancelled on collision with a write action`() {
    prepareFiles()
    val projectAware = BazelProjectAware(project)

    shouldThrow<CancellationException> {
      runInBackgroundWithWriteLockTaken { projectAware.settingsFiles }
    }
  }

  @Test
  fun `should not cache anything when failed`() {
    prepareFiles()
    val projectAware = BazelProjectAware(project)
    val syncCache = SyncCache.getInstance(project)

    syncCache.isAlreadyComputed(projectAware.cachedBazelFiles).shouldBeFalse()
    shouldThrow<CancellationException> {
      runInBackgroundWithWriteLockTaken { projectAware.settingsFiles }
    }
    syncCache.isAlreadyComputed(projectAware.cachedBazelFiles).shouldBeFalse()
  }

  private fun prepareFiles() {
    project.rootDir.apply {
      createFile("MODULE.bazel")
      createFile(".bazelrc")
    }
  }

  private fun <T: Any> runInBackgroundWithWriteLockTaken(action: () -> T) =
    runTestWriteAction {
      withContext(Dispatchers.Default) { action() }
    }

  private fun VirtualFile.createFile(name: String): VirtualFile {
    if (!this.isDirectory) error("Can't create a file in a non-directory file")
    return runTestWriteAction {
      this.createChildData(this, name)
    }
  }
}
