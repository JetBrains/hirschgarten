package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.utils.vfs.refreshAndGetVirtualDirectory
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
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
    project.isBazelProject = true
    project.rootDir = tempDir.refreshAndFindVirtualDirectory()!!
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
    writeAction {
      BazelSymlinkExcludeFileListener().before(listOf(fileCreateEvent))
    }
    val bazelSymlinksToExclude = BazelSymlinkExcludeService.getInstance(project).getOrComputeBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  @Test
  fun `should not exclude symlink when it is not a bazel symlink`() {
    // GIVEN
    val realDirectory = tempDir.resolve("execroot/bazel-out")
    Files.createDirectories(realDirectory)
    val convenientSymlink = tempDir.resolve("not-a-bazel-symlink")
    Files.createSymbolicLink(convenientSymlink, realDirectory)

    val fileCreateEvent = createFakeFileCreateEvent(realDirectory, "not-a-bazel-symlink")

    // WHEN
    BazelSymlinkExcludeFileListener().before(listOf(fileCreateEvent))
    val bazelSymlinksToExclude = BazelSymlinkExcludeService.getInstance(project).getOrComputeBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(emptyList<Path>(), bazelSymlinksToExclude)
  }

  private fun createFakeFileCreateEvent(realDirectory: Path, symlinkName: String): VFileCreateEvent {
    val parentDirectory = tempDir.refreshAndGetVirtualDirectory()
    val fileAttributes = FileAttributes(true, false, true, false, 0, 0, true)
    return VFileCreateEvent(this, parentDirectory, symlinkName, true, fileAttributes, realDirectory.toString(), null)
  }
}
