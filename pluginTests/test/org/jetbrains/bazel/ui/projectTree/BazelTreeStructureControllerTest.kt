package org.jetbrains.bazel.ui.projectTree

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.EXCLUDED
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.ROOT
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.UNIMPORTED
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntityFixtures.emptyBazelDirectoryWorkspaceEntity
import org.jetbrains.bazel.workspacemodel.entities.NonIndexableVirtualFileUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

@TestApplication
internal class BazelTreeStructureControllerTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  private val projectFixture = projectFixture(pathFixture = tempDirFixture)
  private val project by projectFixture

  @BeforeEach
  fun initializeBazelProject() {
    initializeBazelProject(project, tempDir)
  }

  @Nested
  inner class ShouldShowNodeCases {
    @Test
    fun `when project is not imported then only root node should be visible`() {
      // GIVEN
      assertNull(project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(true, ROOT)
      assertNodeVisible(false, UNIMPORTED)
      assertNodeVisible(false, EXCLUDED)
    }

    @Test
    fun `when there are no unimported directories then unimported node is not visible`() {
      // GIVEN
      updateWorkspaceModel()
      assertNotNull(project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(false, UNIMPORTED)
    }

    @Test
    fun `when there is an unimported directory then unimported node is visible`() {
      // GIVEN
      createDirectory("unimportedDir")
      updateWorkspaceModel()
      assertNotNull(project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(true, UNIMPORTED)
    }

    @Test
    fun `when there are no excluded directories then excluded node is not visible`() {
      // GIVEN
      updateWorkspaceModel()
      assertNotNull(project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(false, EXCLUDED)
    }

    @Test
    fun `when there is an excluded directory then excluded node is visible`() {
      // GIVEN
      updateWorkspaceModel(excluded = setOf(createDirectory("excludedDir")))
      assertNotNull(project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(true, EXCLUDED)
    }
  }

  @Nested
  inner class ShouldShowDirectoryUnderNodeCases {
    @Test
    fun `when the project is not imported then all directories are visible under the root node`() {
      // GIVEN
      val exampleDirectory = createDirectory("exampleDirectory")
      assertNull(project.bazelProjectDirectoriesEntity())

      // THEN
      assertVisibleUnderNode(exampleDirectory, ROOT)
    }

    @Test
    fun `individual directories are only visible under the appropriate nodes`() {
      // GIVEN
      val includedDir = createDirectory("includedDir")
      val excludedDir = createDirectory("excludedDir")
      val unimportedDir = createDirectory("unimportedDir")
      updateWorkspaceModel(
        included = setOf(includedDir),
        excluded = setOf(excludedDir),
      )

      // THEN
      assertVisibleOnlyUnderNode(includedDir, ROOT)
      assertVisibleOnlyUnderNode(excludedDir, EXCLUDED)
      assertVisibleOnlyUnderNode(unimportedDir, UNIMPORTED)
    }

    @Test
    fun `parent directory is visible under a node when it contains directory belonging to that node`() {
      // GIVEN
      val parentDir = createDirectory("parentDir")
      updateWorkspaceModel(
        included = setOf(createDirectory("parentDir/includedDir")),
        excluded = setOf(createDirectory("parentDir/excludedDir")),
      )
      createDirectory("parentDir/unimportedDir")

      // THEN parentDir is visible under ROOT, EXCLUDED, UNIMPORTED nodes, because it contains included, excluded and unimported directories
      assertVisibleUnderNode(parentDir, ROOT)
      assertVisibleUnderNode(parentDir, EXCLUDED)
      assertVisibleUnderNode(parentDir, UNIMPORTED)
    }
  }

  @Nested
  inner class ShouldShowFileUnderNodeCases {
    @Test
    fun `files in root directory are visible only under the root node`() {
      // GIVEN
      val file = createFile("README.md")
      updateWorkspaceModel()

      // THEN
      assertVisibleOnlyUnderNode(file, ROOT)
    }

    @Test
    fun `files in directory showing under multiple nodes should be visible only under a single node`() {
      // GIVEN
      val excludedParentDir = createDirectory("parentDir")
      val includedDir = createDirectory("parentDir/includedDir")
      val file = createFile("parentDir/README.md")
      updateWorkspaceModel(
        included = setOf(includedDir),
        excluded = setOf(excludedParentDir),
      )

      // THEN - parentDir is visible under ROOT and EXCLUDED, because it's excluded and contains included directory
      assertVisibleUnderNode(excludedParentDir, ROOT)
      assertVisibleUnderNode(excludedParentDir, EXCLUDED)

      // AND included dir is visible only under ROOT
      assertVisibleOnlyUnderNode(includedDir, ROOT)

      // AND file is visible only under ROOT to avoid duplication
      assertVisibleOnlyUnderNode(file, ROOT)
    }
  }

  private fun createFile(path: String): VirtualFile {
    val virtualDirectory = tempDir.refreshAndFindVirtualDirectory()!!
    return runWriteAction {
      virtualDirectory.findOrCreateFile(path)
    }
  }

  private fun createDirectory(path: String): VirtualFile {
    val virtualDirectory = tempDir.refreshAndFindVirtualDirectory()!!
    return runWriteAction {
      virtualDirectory.findOrCreateDirectory(path)
    }
  }

  private fun updateWorkspaceModel(
    included: Set<VirtualFile> = emptySet(),
    excluded: Set<VirtualFile> = emptySet(),
  ) {
    val workspaceModel = project.workspaceModel
    val workspaceModelUrlManager = workspaceModel.getVirtualFileUrlManager()

    val newWorkspaceEntity = emptyBazelDirectoryWorkspaceEntity(project).also { entity ->
      entity.includedRoots = included.map { it.toVirtualFileUrl(workspaceModelUrlManager) }
        .map { NonIndexableVirtualFileUrl(it) }
        .toMutableList()
      entity.excludedRoots = excluded.map { it.toVirtualFileUrl(workspaceModelUrlManager) }
        .map { NonIndexableVirtualFileUrl(it) }
        .toMutableList()
    }

    runWriteAction {
      workspaceModel.updateProjectModel("Add bazel project directories entity") { storage ->
        storage.addEntity(newWorkspaceEntity)
      }
    }
  }

  private fun assertNodeVisible(expected: Boolean, nodeType: BazelTreeNodeType) {
    val bazelTreeStructureController = BazelTreeStructureController.getInstance(project)
    assertEquals(expected, bazelTreeStructureController.shouldShowNode(nodeType))
  }

  private fun assertVisibleUnderNode(file: VirtualFile, nodeType: BazelTreeNodeType) {
    val bazelTreeStructureController = BazelTreeStructureController.getInstance(project)
    assertTrue(bazelTreeStructureController.shouldShowUnderTreeNode(file, nodeType)) {
      "File $file should be visible under $nodeType node"
    }
  }

  private fun assertVisibleOnlyUnderNode(file: VirtualFile, nodeType: BazelTreeNodeType) {
    val bazelTreeStructureController = BazelTreeStructureController.getInstance(project)
    assertTrue(bazelTreeStructureController.shouldShowUnderTreeNode(file, nodeType)) {
      "File $file should be visible under $nodeType node"
    }
    BazelTreeNodeType.entries
      .filterNot { it == nodeType }
      .forEach { type ->
        assertFalse(bazelTreeStructureController.shouldShowUnderTreeNode(file, type)) {
          "File $file should not be visible under $type node"
        }
      }
  }
}
