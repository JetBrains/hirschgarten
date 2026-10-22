package org.jetbrains.bazel.ui.projectTree

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.createDirectory
import org.jetbrains.bazel.test.framework.testproject.projectDirectories
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.EXCLUDED
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.ROOT
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.UNIMPORTED
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

@BazelTestApplication
internal class BazelTreeStructureControllerTest {
  private val testBazel by testBazelProjectFixture()

  @Nested
  inner class ShouldShowNodeCases {
    @Test
    fun `when project is not imported then only root node should be visible`() {
      // GIVEN
      assertNull(testBazel.project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(true, ROOT)
      assertNodeVisible(false, UNIMPORTED)
      assertNodeVisible(false, EXCLUDED)
    }

    @Test
    fun `when there are no unimported directories then unimported node is not visible`() {
      // GIVEN
      testBazel.projectDirectories()
      assertNotNull(testBazel.project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(false, UNIMPORTED)
    }

    @Test
    fun `when there is an unimported directory then unimported node is visible`() {
      // GIVEN
      testBazel.createDirectory("unimportedDir")
      testBazel.projectDirectories()
      assertNotNull(testBazel.project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(true, UNIMPORTED)
    }

    @Test
    fun `when there are no excluded directories then excluded node is not visible`() {
      // GIVEN
      testBazel.projectDirectories()
      assertNotNull(testBazel.project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(false, EXCLUDED)
    }

    @Test
    fun `when there is an excluded directory then excluded node is visible`() {
      // GIVEN
      testBazel.createDirectory("excludedDir")
      testBazel.projectDirectories { exclude("excludedDir") }
      assertNotNull(testBazel.project.bazelProjectDirectoriesEntity())

      // THEN
      assertNodeVisible(true, EXCLUDED)
    }
  }

  @Nested
  inner class ShouldShowDirectoryUnderNodeCases {
    @Test
    fun `when the project is not imported then all directories are visible under the root node`() {
      // GIVEN
      val exampleDirectory = testBazel.createDirectory("exampleDirectory")
      assertNull(testBazel.project.bazelProjectDirectoriesEntity())

      // THEN
      assertVisibleUnderNode(exampleDirectory, ROOT)
    }

    @Test
    fun `individual directories are only visible under the appropriate nodes`() {
      // GIVEN
      val includedDir = testBazel.createDirectory("includedDir")
      val excludedDir = testBazel.createDirectory("excludedDir")
      val unimportedDir = testBazel.createDirectory("unimportedDir")
      testBazel.projectDirectories {
        include("includedDir")
        exclude("excludedDir")
      }

      // THEN
      assertVisibleOnlyUnderNode(includedDir, ROOT)
      assertVisibleOnlyUnderNode(excludedDir, EXCLUDED)
      assertVisibleOnlyUnderNode(unimportedDir, UNIMPORTED)
    }

    @Test
    fun `parent directory is visible under a node when it contains directory belonging to that node`() {
      // GIVEN
      val parentDir = testBazel.createDirectory("parentDir")
      testBazel.createDirectory("parentDir/includedDir")
      testBazel.createDirectory("parentDir/excludedDir")
      testBazel.projectDirectories {
        include("parentDir/includedDir")
        exclude("parentDir/excludedDir")
      }
      testBazel.createDirectory("parentDir/unimportedDir")

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
      val file = testBazel.writeFile("README.md")
      testBazel.projectDirectories()

      // THEN
      assertVisibleOnlyUnderNode(file, ROOT)
    }

    @Test
    fun `files in directory showing under multiple nodes should be visible only under a single node`() {
      // GIVEN
      val excludedParentDir = testBazel.createDirectory("parentDir")
      val includedDir = testBazel.createDirectory("parentDir/includedDir")
      val file = testBazel.writeFile("parentDir/README.md")
      testBazel.projectDirectories {
        include("parentDir/includedDir")
        exclude("parentDir")
      }

      // THEN - parentDir is visible under ROOT and EXCLUDED, because it's excluded and contains included directory
      assertVisibleUnderNode(excludedParentDir, ROOT)
      assertVisibleUnderNode(excludedParentDir, EXCLUDED)

      // AND included dir is visible only under ROOT
      assertVisibleOnlyUnderNode(includedDir, ROOT)

      // AND file is visible only under ROOT to avoid duplication
      assertVisibleOnlyUnderNode(file, ROOT)
    }
  }

  private fun assertNodeVisible(expected: Boolean, nodeType: BazelTreeNodeType) {
    val bazelTreeStructureController = BazelTreeStructureController.getInstance(testBazel.project)
    assertEquals(expected, bazelTreeStructureController.shouldShowNode(nodeType))
  }

  private fun assertVisibleUnderNode(file: VirtualFile, nodeType: BazelTreeNodeType) {
    val bazelTreeStructureController = BazelTreeStructureController.getInstance(testBazel.project)
    assertTrue(bazelTreeStructureController.shouldShowUnderTreeNode(file, nodeType)) {
      "File $file should be visible under $nodeType node"
    }
  }

  private fun assertVisibleOnlyUnderNode(file: VirtualFile, nodeType: BazelTreeNodeType) {
    val bazelTreeStructureController = BazelTreeStructureController.getInstance(testBazel.project)
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
