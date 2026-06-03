package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.backgroundWriteAction
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.utils.vfs.refreshAndGetVirtualDirectory
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

@TestApplication
class BazelSymlinkExcludeFileListenerTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  private val projectFixture = projectFixture(pathFixture = tempDirFixture, openAfterCreation = true)
  private val project by projectFixture

  @BeforeEach
  fun setUp() {
    initializeBazelProject(project, tempDir)
  }

  @Test
  fun `should exclude bazel symlink when file create event was called`() = runBlocking {
    // GIVEN
    val realDirectory = tempDir.resolve("execroot/bazel-out")
    Files.createDirectories(realDirectory)
    val convenientSymlink = tempDir.resolve("bazel-out")
    Files.createSymbolicLink(convenientSymlink, realDirectory)

    val fileCreateEvent = createFakeFileCreateEvent(realDirectory, "bazel-out")

    // WHEN
    backgroundWriteAction { RefreshQueue.getInstance().processEvents(false, listOf(fileCreateEvent)) }
    val bazelSymlinksToExclude = BazelSymlinkExcludeService.getInstance(project).getBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  @Test
  fun `should not exclude symlink when it is not a bazel symlink`() = runBlocking {
    // GIVEN
    val realDirectory = tempDir.resolve("execroot/bazel-out")
    Files.createDirectories(realDirectory)
    val convenientSymlink = tempDir.resolve("not-a-bazel-symlink")
    Files.createSymbolicLink(convenientSymlink, realDirectory)

    val fileCreateEvent = createFakeFileCreateEvent(realDirectory, "not-a-bazel-symlink")

    // WHEN
    backgroundWriteAction { RefreshQueue.getInstance().processEvents(false, listOf(fileCreateEvent)) }
    val bazelSymlinksToExclude = BazelSymlinkExcludeService.getInstance(project).getBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(emptyList<Path>(), bazelSymlinksToExclude)
  }

  private fun createFakeFileCreateEvent(realDirectory: Path, symlinkName: String): VFileCreateEvent {
    val parentDirectory = tempDir.refreshAndGetVirtualDirectory()
    val fileAttributes = FileAttributes(true, false, true, false, 0, 0, true)
    return VFileCreateEvent(this, parentDirectory, symlinkName, true, fileAttributes, realDirectory.toString(), null)
  }
}
