package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.utils.vfs.refreshAndGetVirtualDirectory
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.symlinks.createBazelConvenienceSymlink
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.uninstall
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path

@BazelTestApplication
class BazelDirectoryIndexExcludePolicyTest {
  private val testBazel by testBazelProjectFixture(
    openAfterCreation = false,
    beforeInstall = { root, _ -> root.createBazelConvenienceSymlink("bazel-out") },
  )

  private val convenienceSymlink: Path
    get() = testBazel.workspaceRoot.resolve("bazel-out")

  @ParameterizedTest(name = "isBazelProject = {0}")
  @ValueSource(booleans = [true, false])
  fun `should mark symlinks registered in the service as excluded from the project`(isBazelProject: Boolean) = runBlocking {
    // GIVEN
    if (!isBazelProject) {
      testBazel.uninstall()
    }
    edtWriteAction {
      BazelSymlinkExcludeService.getInstance(testBazel.project).addBazelSymlinksToExclude(setOf(convenienceSymlink))
    }

    // WHEN
    val isExcluded = isExcluded(convenienceSymlink.refreshAndGetVirtualDirectory())

    // THEN
    assertTrue(isExcluded)
  }

  @Test
  fun `should flush workspace file index cache after updating symlinks`() = runBlocking {
    // GIVEN workspace file index cache is populated
    val convenienceSymlinkFile = convenienceSymlink.refreshAndGetVirtualDirectory()
    assertFalse(isExcluded(convenienceSymlinkFile))

    // AND convenience symlink has been excluded
    edtWriteAction {
      BazelSymlinkExcludeService.getInstance(testBazel.project).addBazelSymlinksToExclude(setOf(convenienceSymlink))
    }

    // WHEN the state of the file has been queried again
    val isExcluded = isExcluded(convenienceSymlinkFile)

    // THEN the cache has been flushed and symlink is now excluded
    assertTrue(isExcluded)
  }

  private suspend fun isExcluded(file: VirtualFile) = readAction {
    ProjectFileIndex.getInstance(testBazel.project).isExcluded(file)
  }
}
