package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.edtWriteAction
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.refreshVfs
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntityFixtures.emptyBazelDirectoryWorkspaceEntity
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
    initializeBazelProject(project, tempDir)
    Files.createFile(tempDir.resolve("MODULE.bazel"))
  }

  @Test
  fun `should compute list of bazel convenience symlinks`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(project)
    val convenientSymlink = createConvenientSymlink("bazel-bin")

    // WHEN
    val bazelSymlinksToExclude = bazelSymlinkExcludeService.scanForBazelSymlinksToExclude(project.rootDir.toNioPath())

    // THEN
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  @Test
  fun `should add a new symlink to the list`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(project)
    val convenientSymlink = createConvenientSymlink("bazel-out")

    // WHEN
    edtWriteAction {
      bazelSymlinkExcludeService.addBazelSymlinksToExclude(setOf(convenientSymlink))
    }
    val bazelSymlinksToExclude = bazelSymlinkExcludeService.getBazelSymlinksToExclude()

    // THEN
    assertIterableEquals(listOf(convenientSymlink), bazelSymlinksToExclude)
  }

  @Test
  fun `should add a new symlink to bazel workspace model`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(project)
    val convenientSymlink = createConvenientSymlink("bazel-out")
    edtWriteAction {
      bazelSymlinkExcludeService.addBazelSymlinksToExclude(setOf(convenientSymlink))
    }

    val workspaceModel = WorkspaceModel.getInstance(project)
    workspaceModel.update("Initialize empty workspace entity for test") { mutableEntityStorage ->
      mutableEntityStorage.addEntity(emptyBazelDirectoryWorkspaceEntity(project))
    }

    // WHEN
    edtWriteAction {
      bazelSymlinkExcludeService.refreshWorkspaceModel()
    }

    // THEN
    val actualPaths = project.bazelProjectDirectoriesEntity()!!.excludedRoots.mapNotNull { it.url.virtualFile?.toNioPath() }
    assertIterableEquals(listOf(convenientSymlink), actualPaths)
  }

  private fun createConvenientSymlink(name: String): Path {
    val realDirectory = tempDir.resolve("execroot/$name")
    Files.createDirectories(realDirectory)
    val convenientSymlink = tempDir.resolve(name)
    Files.createSymbolicLink(convenientSymlink, realDirectory)
    tempDir.refreshVfs()
    return convenientSymlink
  }
}
