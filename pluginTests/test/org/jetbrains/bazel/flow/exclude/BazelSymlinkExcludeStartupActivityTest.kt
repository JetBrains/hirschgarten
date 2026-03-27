package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.utils.vfs.refreshAndGetVirtualDirectory
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntityFixtures.emptyBazelDirectoryWorkspaceEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

@TestApplication
internal class BazelSymlinkExcludeStartupActivityTest {
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
  fun `should scan for bazel symlinks on project startup and update workspace model`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(project)
    val convenientSymlink = createConvenientSymlink("bazel-bin")

    project.workspaceModel.update("Initialize empty workspace entity for test") { mutableEntityStorage ->
      mutableEntityStorage.addEntity(emptyBazelDirectoryWorkspaceEntity(project))
    }

    // WHEN
    BazelSymlinkExcludeStartupActivity().execute(project)

    // THEN
    assertEquals(setOf(convenientSymlink), bazelSymlinkExcludeService.getBazelSymlinksToExclude())

    // AND
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
}
