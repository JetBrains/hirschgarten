package org.jetbrains.bazel.flow

import com.intellij.configurationStore.ProjectStorePathCustomizer
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project.DIRECTORY_STORE_FOLDER
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.DOT_BAZELBSP_DIR_NAME
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.writeText

@TestApplication
internal class BazelProjectStorePathCustomizerTest {
  private val tempDir by tempPathFixture()

  private val rootDir: Path by lazy { tempDir.resolve("project").createDirectories() }
  private val outsideDir: Path by lazy { tempDir.resolve("outside").createDirectories() }
  private val workspaceFile: Path by lazy { rootDir.resolve("MODULE.bazel").apply { writeText("") } }

  @Test
  fun `uses the default dot idea directory without the section`() {
    writeProjectView("")

    val descriptor = storeDescriptor()

    descriptor.dotIdea shouldBe descriptor.historicalProjectBasePath.resolve(DIRECTORY_STORE_FOLDER)
  }

  @Test
  fun `uses a dot idea directory inside of the project`() {
    writeProjectView("dot_idea_directory_location: custom/.idea")

    val descriptor = storeDescriptor()

    descriptor.dotIdea shouldBe descriptor.historicalProjectBasePath.resolve("custom").resolve(DIRECTORY_STORE_FOLDER)
  }

  @Test
  fun `does not use an absolute dot idea directory outside of the project`() {
    writeProjectView("dot_idea_directory_location: ${outsideDir.resolve(DIRECTORY_STORE_FOLDER)}")

    val descriptor = storeDescriptor()

    descriptor.dotIdea shouldBe descriptor.historicalProjectBasePath.resolve(DIRECTORY_STORE_FOLDER)
  }

  @Test
  fun `does not use a relative dot idea directory outside of the project`() {
    writeProjectView("dot_idea_directory_location: ../outside/.idea")

    val descriptor = storeDescriptor()

    descriptor.dotIdea shouldBe descriptor.historicalProjectBasePath.resolve(DIRECTORY_STORE_FOLDER)
  }

  @Test
  fun `does not use a dot idea directory behind a symlink outside of the project`() {
    rootDir.resolve("link").createSymbolicLinkPointingTo(outsideDir)
    writeProjectView("dot_idea_directory_location: link/.idea")

    val descriptor = storeDescriptor()

    descriptor.dotIdea shouldBe descriptor.historicalProjectBasePath.resolve(DIRECTORY_STORE_FOLDER)
  }

  @Test
  fun `does not use a dot idea directory behind a dangling symlink`() {
    rootDir.resolve("link").createSymbolicLinkPointingTo(outsideDir.resolve("missing"))
    writeProjectView("dot_idea_directory_location: link/.idea")

    val descriptor = storeDescriptor()

    descriptor.dotIdea shouldBe descriptor.historicalProjectBasePath.resolve(DIRECTORY_STORE_FOLDER)
  }

  @Test
  fun `does not use a dot idea directory outside of the project whose real path is inside of the project`() {
    outsideDir.resolve("link").createSymbolicLinkPointingTo(rootDir.resolve("custom").createDirectories())
    writeProjectView("dot_idea_directory_location: ${outsideDir.resolve("link").resolve(DIRECTORY_STORE_FOLDER)}")

    val descriptor = storeDescriptor()

    descriptor.dotIdea shouldBe descriptor.historicalProjectBasePath.resolve(DIRECTORY_STORE_FOLDER)
  }

  @Test
  fun `does not read an imported project view outside of the project`() {
    outsideDir.resolve("outside.bazelproject").writeText("dot_idea_directory_location: custom/.idea")
    writeProjectView("import ../outside/outside.bazelproject")

    val descriptor = storeDescriptor()

    descriptor.dotIdea shouldBe descriptor.historicalProjectBasePath.resolve(DIRECTORY_STORE_FOLDER)
  }

  @Test
  fun `reads an imported project view inside of the project`() {
    rootDir.resolve("shared.bazelproject").writeText("dot_idea_directory_location: custom/.idea")
    writeProjectView("import shared.bazelproject")

    val descriptor = storeDescriptor()

    descriptor.dotIdea shouldBe descriptor.historicalProjectBasePath.resolve("custom").resolve(DIRECTORY_STORE_FOLDER)
  }

  private fun writeProjectView(text: String) {
    rootDir.resolve(DOT_BAZELBSP_DIR_NAME).createDirectories().resolve(DEFAULT_PROJECT_VIEW_FILE_NAME).writeText(text)
  }

  private fun storeDescriptor(): BazelProjectStoreDescriptor =
    ExtensionPointName<ProjectStorePathCustomizer>("com.intellij.projectStorePathCustomizer").extensionList
      .firstNotNullOf { it.getStoreDirectoryPath(workspaceFile) as? BazelProjectStoreDescriptor }
}
