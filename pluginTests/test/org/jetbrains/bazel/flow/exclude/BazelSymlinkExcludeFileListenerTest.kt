package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.backgroundWriteAction
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.util.io.IoTestUtil
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.testFramework.utils.vfs.refreshAndGetVirtualDirectory
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.symlinks.createBazelConvenienceSymlink
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.readAttributes

@BazelTestApplication
class BazelSymlinkExcludeFileListenerTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `should exclude bazel symlink when file create event was called`() = runBlocking {
    // GIVEN
    val convenientSymlink = testBazel.workspaceRoot.createBazelConvenienceSymlink( "bazel-out")
    val realDirectory = testBazel.workspaceRoot.resolve("execroot/bazel-out")

    val fileCreateEvent = createFakeFileCreateEvent(realDirectory, "bazel-out")

    // WHEN
    backgroundWriteAction { RefreshQueue.getInstance().processEvents(false, listOf(fileCreateEvent)) }
    val bazelSymlinksToExclude = BazelSymlinkExcludeService.getInstance(testBazel.project).getBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  fun `should exclude bazel junction when file create event was called`() = runBlocking {
    // GIVEN
    val realDirectory = testBazel.workspaceRoot.resolve("execroot/bazel-out")
    Files.createDirectories(realDirectory)
    val junction = IoTestUtil.createJunction(realDirectory.toString(), testBazel.workspaceRoot.resolve("bazel-out").toString()).toPath()

    val parentDirectory = testBazel.workspaceRoot.refreshAndGetVirtualDirectory()
    val junctionAttributes = FileAttributes.fromNio(
      junction,
      junction.readAttributes(LinkOption.NOFOLLOW_LINKS),
    )
    val fileCreateEvent = VFileCreateEvent(this, parentDirectory, "bazel-out", true, junctionAttributes, realDirectory.toString(), null)

    // WHEN
    backgroundWriteAction { RefreshQueue.getInstance().processEvents(false, listOf(fileCreateEvent)) }
    val bazelSymlinksToExclude = BazelSymlinkExcludeService.getInstance(testBazel.project).getBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(listOf(junction), bazelSymlinksToExclude)
  }

  @Test
  fun `should not exclude symlink when it is not a bazel symlink`() = runBlocking {
    // GIVEN
    testBazel.workspaceRoot.createBazelConvenienceSymlink("not-a-bazel-symlink")
    val realDirectory = testBazel.workspaceRoot.resolve("execroot/not-a-bazel-symlink")

    val fileCreateEvent = createFakeFileCreateEvent(realDirectory, "not-a-bazel-symlink")

    // WHEN
    backgroundWriteAction { RefreshQueue.getInstance().processEvents(false, listOf(fileCreateEvent)) }
    val bazelSymlinksToExclude = BazelSymlinkExcludeService.getInstance(testBazel.project).getBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(emptyList<Path>(), bazelSymlinksToExclude)
  }

  @Test
  fun `should not exclude a new directory that is not a symlink`() = runBlocking {
    // GIVEN a plain directory with the name of a convenience symlink
    val plainDirectory = testBazel.workspaceRoot.resolve("bazel-out")
    Files.createDirectories(plainDirectory)

    val parentDirectory = testBazel.workspaceRoot.refreshAndGetVirtualDirectory()
    val directoryAttributes = FileAttributes(true, false, false, false, 0, 0, true)
    val fileCreateEvent = VFileCreateEvent(this, parentDirectory, "bazel-out", true, directoryAttributes, null, null)

    // WHEN
    backgroundWriteAction { RefreshQueue.getInstance().processEvents(false, listOf(fileCreateEvent)) }
    val bazelSymlinksToExclude = BazelSymlinkExcludeService.getInstance(testBazel.project).getBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(emptyList<Path>(), bazelSymlinksToExclude)
  }

  private fun createFakeFileCreateEvent(realDirectory: Path, symlinkName: String): VFileCreateEvent {
    val parentDirectory = testBazel.workspaceRoot.refreshAndGetVirtualDirectory()
    val fileAttributes = FileAttributes(true, false, true, false, 0, 0, true)
    return VFileCreateEvent(this, parentDirectory, symlinkName, true, fileAttributes, realDirectory.toString(), null)
  }
}
