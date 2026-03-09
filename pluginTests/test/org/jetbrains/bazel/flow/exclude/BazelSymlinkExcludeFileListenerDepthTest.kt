package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.utils.vfs.refreshAndGetVirtualDirectory
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

@TestApplication
class BazelSymlinkExcludeFileListenerDepthTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  private val projectFixture = projectFixture(pathFixture = tempDirFixture, openAfterCreation = true)
  private val project by projectFixture

  private var originalDepth: Int = 2

  @BeforeEach
  fun setUp() {
    project.isBazelProject = true
    project.rootDir = tempDir.refreshAndFindVirtualDirectory()!!
    originalDepth = Registry.intValue("bazel.symlink.scan.max.depth")
  }

  @AfterEach
  fun tearDown() {
    Registry.get("bazel.symlink.scan.max.depth").setValue(originalDepth.toString())
  }

  @Test
  fun `should not exclude symlinks when scanning is disabled via depth=0`() = runBlocking {
    // GIVEN: scanning is disabled (depth=0), but a bazel symlink exists on disk
    Registry.get("bazel.symlink.scan.max.depth").setValue("0")

    val realDirectory = tempDir.resolve("execroot/bazel-out")
    Files.createDirectories(realDirectory)
    Files.createSymbolicLink(tempDir.resolve("bazel-out"), realDirectory)

    val fileCreateEvent = createFakeFileCreateEvent(tempDir, "bazel-out")

    // WHEN: VFS listener fires for the new symlink
    writeAction {
      BazelSymlinkExcludeFileListener().before(listOf(fileCreateEvent))
    }
    val excluded = BazelSymlinkExcludeService.getInstance(project).getOrComputeBazelSymlinksToExclude()

    // THEN: symlink should NOT be excluded because scanning is disabled
    // BUG: this assertion fails because the listener bypasses the depth setting
    assertIterableEquals(emptyList<Path>(), excluded)
  }

  @Test
  fun `should not exclude symlinks from nested workspace beyond max depth`() = runBlocking {
    // GIVEN: max depth=1, but a bazel symlink exists in a nested workspace at depth 3
    Registry.get("bazel.symlink.scan.max.depth").setValue("1")

    val nestedWorkspace = tempDir.resolve("testdata/nested-bazel")
    val realDirectory = nestedWorkspace.resolve("execroot/bazel-out")
    Files.createDirectories(realDirectory)
    Files.createSymbolicLink(nestedWorkspace.resolve("bazel-out"), realDirectory)

    val nestedVDir = nestedWorkspace.refreshAndGetVirtualDirectory()
    val fileAttributes = FileAttributes(true, false, true, false, 0, 0, true)
    val fileCreateEvent = VFileCreateEvent(this, nestedVDir, "bazel-out", true, fileAttributes, realDirectory.toString(), null)

    // WHEN: VFS listener fires for the nested symlink
    writeAction {
      BazelSymlinkExcludeFileListener().before(listOf(fileCreateEvent))
    }
    val excluded = BazelSymlinkExcludeService.getInstance(project).getOrComputeBazelSymlinksToExclude()

    // THEN: symlink should NOT be excluded because it's at depth 3 (testdata/nested-bazel/bazel-out)
    //       and max depth is 1
    // BUG: this assertion fails because the listener doesn't check depth at all
    assertIterableEquals(emptyList<Path>(), excluded)
  }

  private fun createFakeFileCreateEvent(parentPath: Path, symlinkName: String): VFileCreateEvent {
    val parentDirectory = parentPath.refreshAndGetVirtualDirectory()
    val realPath = parentPath.resolve("execroot/$symlinkName")
    val fileAttributes = FileAttributes(true, false, true, false, 0, 0, true)
    return VFileCreateEvent(this, parentDirectory, symlinkName, true, fileAttributes, realPath.toString(), null)
  }
}
