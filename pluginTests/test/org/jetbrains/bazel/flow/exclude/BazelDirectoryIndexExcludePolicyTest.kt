package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.readAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.utils.vfs.refreshAndGetVirtualDirectory
import io.kotest.common.runBlocking
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.flow.open.initProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

@TestApplication
class BazelDirectoryIndexExcludePolicyTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  private val projectFixture = projectFixture(pathFixture = tempDirFixture)
  private val project by projectFixture

  private lateinit var convenienceSymlink: Path

  @BeforeEach
  fun setUp() {
    project.initProperties(tempDir)
    val realDirectory = tempDir.resolve("execroot/bazel-out")
    Files.createDirectories(realDirectory)
    convenienceSymlink = tempDir.resolve("bazel-out")
    Files.createSymbolicLink(convenienceSymlink, realDirectory)
  }

  @Test
  fun `should mark symlinks registered in the service as excluded from the project`() {
    // GIVEN
    BazelSymlinkExcludeService.getInstance(project).addBazelSymlinksToExclude(setOf(convenienceSymlink))

    // WHEN
    val isExcluded = runBlocking {
      readAction {
        ProjectFileIndex.getInstance(project).isExcluded(convenienceSymlink.refreshAndGetVirtualDirectory())
      }
    }

    // THEN
    assertTrue(isExcluded)
  }

  @Test
  fun `should not exclude files from non-bazel projects`() {
    // GIVEN
    project.isBazelProject = false
    BazelSymlinkExcludeService.getInstance(project).addBazelSymlinksToExclude(setOf(convenienceSymlink))

    // WHEN
    val isExcluded = runBlocking {
      readAction {
        ProjectFileIndex.getInstance(project).isExcluded(convenienceSymlink.refreshAndGetVirtualDirectory())
      }
    }

    // THEN
    assertFalse(isExcluded)
  }
}
