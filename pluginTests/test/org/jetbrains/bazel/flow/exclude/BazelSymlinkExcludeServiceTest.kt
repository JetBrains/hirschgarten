package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.utils.vfs.refreshAndGetVirtualDirectory
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
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
    project.isBazelProject = true
    project.rootDir = tempDir.refreshAndFindVirtualDirectory()!!
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
  fun `should add a new symlink to the list`() = runBlocking {
    // GIVEN
    val symlinkExcludeService = BazelSymlinkExcludeService.getInstance(project)
    val convenientSymlink = createConvenientSymlink("bazel-out")

    // WHEN
    writeAction {
      symlinkExcludeService.addBazelSymlinksToExclude(setOf(convenientSymlink))
    }
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

  @Test
  fun `should add a new symlink to bazel workspace model`() = runBlocking {
    // GIVEN
    val symlinkExcludeService = BazelSymlinkExcludeService.getInstance(project)
    val convenientSymlink = createConvenientSymlink("bazel-out")

    val workspaceModel = WorkspaceModel.getInstance(project)
    workspaceModel.update("Initialize empty workspace entity for test") { mutableEntityStorage ->
      mutableEntityStorage.addEntity(emptyBazelDirectoryWorkspaceEntity(workspaceModel))
    }

    // WHEN
    writeAction {
      symlinkExcludeService.addBazelSymlinksToExclude(setOf(convenientSymlink))
    }

    // THEN
    val actualPaths = project.bazelProjectDirectoriesEntity()!!.excludedRoots.mapNotNull { it.virtualFile?.toNioPath() }
    assertIterableEquals(listOf(convenientSymlink), actualPaths)
  }

  private fun createConvenientSymlink(name: String): Path {
    val realDirectory = tempDir.resolve("execroot/$name")
    Files.createDirectories(realDirectory)
    val convenientSymlink = tempDir.resolve(name)
    Files.createSymbolicLink(convenientSymlink, realDirectory)
    return convenientSymlink
  }

  private fun emptyBazelDirectoryWorkspaceEntity(workspaceModel: WorkspaceModel) =
    BazelProjectDirectoriesEntity(
      projectRoot = project.rootDir.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()),
      includedRoots = emptyList(),
      excludedRoots = emptyList(),
      indexAllFilesInIncludedRoots = false,
      indexAdditionalFiles = emptyList(),
      entitySource = BazelProjectEntitySource,
    )
}
