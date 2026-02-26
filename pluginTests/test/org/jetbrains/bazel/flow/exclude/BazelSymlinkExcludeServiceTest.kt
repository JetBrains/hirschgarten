package org.jetbrains.bazel.flow.exclude

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.utils.vfs.refreshAndGetVirtualDirectory
import org.jetbrains.bazel.flow.open.initProperties
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

@TestApplication
class BazelSymlinkExcludeServiceTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  private val projectFixture = projectFixture(pathFixture = tempDirFixture)
  private val project by projectFixture

  @BeforeEach
  fun setUp() {
    project.initProperties(tempDir)
    Files.createFile(tempDir.resolve("MODULE.bazel"))
    tempDir.refreshAndGetVirtualDirectory()
  }

  @Test
  fun `should compute list of bazel convenience symlinks`() {
    // GIVEN
    val convenientSymlink = createConvenientSymlink("bazel-bin")

    // WHEN
    val bazelSymlinksToExclude = BazelSymlinkExcludeService.getInstance(project).getOrComputeBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  @Test
  fun `should add a new symlink to the list`() {
    // GIVEN
    val symlinkExcludeService = BazelSymlinkExcludeService.getInstance(project)
    val convenientSymlink = createConvenientSymlink("bazel-out")

    // WHEN
    symlinkExcludeService.addBazelSymlinksToExclude(setOf(convenientSymlink))
    val bazelSymlinksToExclude = symlinkExcludeService.getOrComputeBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  @Test
  fun `should not cache an empty list of convenience symlinks`() {
    // GIVEN symlinks list is computed when there are no any symlinks
    val symlinkExcludeService = BazelSymlinkExcludeService.getInstance(project)
    assertIterableEquals(emptyList<Path>(), symlinkExcludeService.getOrComputeBazelSymlinksToExclude())

    // AND symlinks has been created after first computation
    val convenientSymlink = createConvenientSymlink("bazel-out")

    // WHEN symlinks list is computed again
    val bazelSymlinksToExclude = symlinkExcludeService.getOrComputeBazelSymlinksToExclude()

    // THEN there is a convenient symlink in the list
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  private fun createConvenientSymlink(name: String): Path {
    val realDirectory = tempDir.resolve("execroot/$name")
    Files.createDirectories(realDirectory)
    val convenientSymlink = tempDir.resolve(name)
    Files.createSymbolicLink(convenientSymlink, realDirectory)
    return convenientSymlink
  }
}
