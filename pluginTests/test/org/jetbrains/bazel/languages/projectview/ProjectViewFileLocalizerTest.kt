package org.jetbrains.bazel.languages.projectview

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.DOT_BAZELBSP_DIR_NAME
import org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.pickProjectViewFileForProject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

@TestApplication
internal class ProjectViewFileLocalizerTest {
  private val tempDir by tempPathFixture()

  private val rootDir: Path by lazy { tempDir.resolve("project").createDirectories() }
  private val outsideDir: Path by lazy { tempDir.resolve("outside").createDirectories() }
  private val workspaceFile: Path by lazy { rootDir.resolve("MODULE.bazel").apply { writeText("") } }

  @Test
  fun `creates the project view file in the dot bazelbsp directory`() {
    val projectViewFile = pickProjectViewFileForProject(workspaceFile, rootDir)

    projectViewFile shouldBe rootDir.resolve(DOT_BAZELBSP_DIR_NAME).resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)
    projectViewFile.isRegularFile() shouldBe true
  }

  @Test
  fun `keeps an existing project view file`() {
    val existingFile = rootDir.resolve(DOT_BAZELBSP_DIR_NAME).createDirectory().resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)
    existingFile.writeText("targets: //...")

    pickProjectViewFileForProject(workspaceFile, rootDir) shouldBe existingFile
    existingFile.readText() shouldBe "targets: //..."
  }

  @Test
  fun `does not write through a dangling symlink`() {
    val target = outsideDir.resolve("target")
    rootDir.resolve(DOT_BAZELBSP_DIR_NAME).createDirectory()
      .resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)
      .createSymbolicLinkPointingTo(target)

    assertThrows<IllegalStateException> { pickProjectViewFileForProject(workspaceFile, rootDir) }
    target.exists() shouldBe false
  }

  @Test
  fun `does not write into a dot bazelbsp directory outside of the project`() {
    rootDir.resolve(DOT_BAZELBSP_DIR_NAME).createSymbolicLinkPointingTo(outsideDir)

    assertThrows<IllegalStateException> { pickProjectViewFileForProject(workspaceFile, rootDir) }
    outsideDir.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME).exists() shouldBe false
  }
}
