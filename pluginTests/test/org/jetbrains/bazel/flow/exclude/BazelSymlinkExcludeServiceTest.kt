package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.util.io.NioFiles
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.refreshVfs
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.createConvenienceSymlink
import org.jetbrains.bazel.test.framework.testproject.projectDirectories
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.name

@BazelTestApplication
class BazelSymlinkExcludeServiceTest {
  private val testBazel by testBazelProjectFixture(openAfterCreation = false)

  @Test
  fun `should compute list of bazel convenience symlinks`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(testBazel.project)
    val convenientSymlink = testBazel.createConvenienceSymlink("bazel-bin")

    // WHEN
    val bazelSymlinksToExclude = bazelSymlinkExcludeService.scanForBazelSymlinksToExclude(testBazel.workspaceRoot)

    // THEN
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  @Test
  fun `should compute bazel convenience symlinks when their targets cannot be accessed`(): Unit = timeoutRunBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(testBazel.project)
    val outputBase = testBazel.workspaceRoot.resolve("missing")
    val execRoot = outputBase.resolve("execroot/_main")
    val convenientSymlinks = setOf(
      testBazel.createConvenienceSymlink("bazel-bin", execRoot.resolve("bazel-out/platform-fastbuild/bin")),
      testBazel.createConvenienceSymlink("bazel-out", execRoot.resolve("bazel-out")),
      testBazel.createConvenienceSymlink("bazel-testlogs", execRoot.resolve("bazel-out/platform-fastbuild/testlogs")),
      testBazel.createConvenienceSymlink("bazel-${testBazel.workspaceRoot.name}", execRoot),
    )
    // A Windows junction needs the target at creation, so remove the whole output base afterwards.
    NioFiles.deleteRecursively(outputBase)
    testBazel.workspaceRoot.refreshVfs()

    // WHEN
    val bazelSymlinksToExclude = bazelSymlinkExcludeService.scanForBazelSymlinksToExclude(testBazel.workspaceRoot)

    // THEN
    assertEquals(convenientSymlinks, bazelSymlinksToExclude)
  }

  @Test
  fun `should add a new symlink to the list`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(testBazel.project)
    val convenientSymlink = testBazel.createConvenienceSymlink("bazel-out")

    // WHEN
    edtWriteAction {
      bazelSymlinkExcludeService.addBazelSymlinksToExclude(setOf(convenientSymlink))
    }
    val bazelSymlinksToExclude = bazelSymlinkExcludeService.getBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  @Test
  fun `should keep the symlinks from an earlier call`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(testBazel.project)
    val binSymlink = testBazel.createConvenienceSymlink("bazel-bin")
    val outSymlink = testBazel.createConvenienceSymlink("bazel-out")

    // WHEN
    edtWriteAction {
      bazelSymlinkExcludeService.addBazelSymlinksToExclude(setOf(binSymlink))
    }
    edtWriteAction {
      bazelSymlinkExcludeService.addBazelSymlinksToExclude(setOf(outSymlink))
    }

    // THEN
    assertEquals(setOf(binSymlink, outSymlink), bazelSymlinkExcludeService.getBazelSymlinksToExclude())
  }

  @Test
  fun `should add a new symlink to bazel workspace model`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(testBazel.project)
    val convenientSymlink = testBazel.createConvenienceSymlink("bazel-out")
    edtWriteAction {
      bazelSymlinkExcludeService.addBazelSymlinksToExclude(setOf(convenientSymlink))
    }

    testBazel.projectDirectories()

    // WHEN
    bazelSymlinkExcludeService.refreshWorkspaceModel()

    // THEN
    val actualPaths = testBazel.project.bazelProjectDirectoriesEntity()!!.excludedRoots.mapNotNull { it.virtualFile?.toNioPath() }
    assertIterableEquals(listOf(convenientSymlink), actualPaths)
  }

  @Test
  fun `should add a symlink to bazel workspace model only once`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(testBazel.project)
    val convenientSymlink = testBazel.createConvenienceSymlink("bazel-out")
    edtWriteAction {
      bazelSymlinkExcludeService.addBazelSymlinksToExclude(setOf(convenientSymlink))
    }

    testBazel.projectDirectories()

    // WHEN the service refreshes the model twice, as the debounced flow can do
    bazelSymlinkExcludeService.refreshWorkspaceModel()
    bazelSymlinkExcludeService.refreshWorkspaceModel()

    // THEN
    val actualPaths = testBazel.project.bazelProjectDirectoriesEntity()!!.excludedRoots.mapNotNull { it.virtualFile?.toNioPath() }
    assertIterableEquals(listOf(convenientSymlink), actualPaths)
  }
}
