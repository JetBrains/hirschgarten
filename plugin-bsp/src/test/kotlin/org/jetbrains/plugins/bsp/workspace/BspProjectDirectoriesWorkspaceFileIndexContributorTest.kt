package org.jetbrains.plugins.bsp.workspace

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.roots.FileIndex
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.testFramework.utils.io.createFile
import com.intellij.testFramework.workspaceModel.updateProjectModel
import com.intellij.util.io.createDirectories
import com.intellij.workspaceModel.ide.toPath
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.workspacemodel.entities.BspProjectDirectoriesEntity
import org.jetbrains.workspacemodel.storage.BspEntitySource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BspProjectDirectoriesWorkspaceFileIndexContributorTest : WorkspaceModelBaseTest() {
  private lateinit var fileIndex: FileIndex
  private lateinit var fileIndexFacade: FileIndexFacade

  private lateinit var projectRoot: VirtualFileUrl
  private lateinit var file: VirtualFileUrl
  private lateinit var dir1: VirtualFileUrl
  private lateinit var dir1Dir2: VirtualFileUrl
  private lateinit var dir1Dir2File: VirtualFileUrl
  private lateinit var dir1Dir3: VirtualFileUrl
  private lateinit var dir1Dir3File: VirtualFileUrl
  private lateinit var dir4: VirtualFileUrl
  private lateinit var dir4File: VirtualFileUrl

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    fileIndex = ProjectRootManager.getInstance(project).fileIndex
    fileIndexFacade = FileIndexFacade.getInstance(project)

    prepareMockProjectFiles()
  }

  private fun prepareMockProjectFiles() {
    projectRoot = projectBasePath.toVirtualFileUrl(virtualFileUrlManager)
    file = projectRoot.createFile("file")
    dir1 = projectRoot.createDir("dir1")
    dir1Dir2 = dir1.createDir("dir2")
    dir1Dir2File = dir1Dir2.createFile("file")
    dir1Dir3 = dir1.createDir("dir3")
    dir1Dir3File = dir1Dir3.createFile("file")
    dir4 = projectRoot.createDir("dir4")
    dir4File = dir4.createFile("file")
  }

  private fun VirtualFileUrl.createDir(name: String): VirtualFileUrl =
    toPath().resolve(name).createDirectories().toVirtualFileUrl(virtualFileUrlManager)

  private fun VirtualFileUrl.createFile(name: String): VirtualFileUrl =
    toPath().createFile(name).toVirtualFileUrl(virtualFileUrlManager)

  @Test
  fun `should exclude all the files in the project if no files are included`() {
    // given
    val entity = BspProjectDirectoriesEntity(
      projectRoot = projectBasePath.toVirtualFileUrl(virtualFileUrlManager),
      includedRoots = emptyList(),
      excludedRoots = emptyList(),
      entitySource = BspEntitySource
    )

    // when
    runTestWriteAction {
      workspaceModel.updateProjectModel { it.addEntity(entity) }
    }

    // then
    emptyList<VirtualFileUrl>().shouldBeExactlyIncluded()

    listOf(
      file,
      dir1,
      dir1Dir2,
      dir1Dir2File,
      dir1Dir3,
      dir1Dir3File,
      dir4,
      dir4File,
    ).shouldBeExcluded()
  }

  @Test
  fun `should include all the files in the project if project root is included`() {
    // given
    val entity = BspProjectDirectoriesEntity(
      projectRoot = projectBasePath.toVirtualFileUrl(virtualFileUrlManager),
      includedRoots = listOf(projectRoot),
      excludedRoots = emptyList(),
      entitySource = BspEntitySource
    )

    // when
    runTestWriteAction {
      workspaceModel.updateProjectModel { it.addEntity(entity) }
    }

    // then
    listOf(
      projectRoot,
      file,
      dir1,
      dir1Dir2,
      dir1Dir2File,
      dir1Dir3,
      dir1Dir3File,
      dir4,
      dir4File,
    ).shouldBeExactlyIncluded()

    emptyList<VirtualFileUrl>().shouldBeExcluded()
  }

  @Test
  fun `should include included directories in the project and exclude not included directories`() {
    // given
    val entity = BspProjectDirectoriesEntity(
      projectRoot = projectBasePath.toVirtualFileUrl(virtualFileUrlManager),
      includedRoots = listOf(dir1),
      excludedRoots = emptyList(),
      entitySource = BspEntitySource
    )

    // when
    runTestWriteAction {
      workspaceModel.updateProjectModel { it.addEntity(entity) }
    }

    // then
    listOf(
      dir1,
      dir1Dir2,
      dir1Dir2File,
      dir1Dir3,
      dir1Dir3File,
    ).shouldBeExactlyIncluded()

    listOf(
      file,
      dir4,
      dir4File,
    ).shouldBeExcluded()
  }

  @Test
  fun `should include included directories in the project and exclude excluded (nested) directories`() {
    // given
    val entity = BspProjectDirectoriesEntity(
      projectRoot = projectBasePath.toVirtualFileUrl(virtualFileUrlManager),
      includedRoots = listOf(dir1),
      excludedRoots = listOf(dir1Dir3, dir4),
      entitySource = BspEntitySource
    )

    // when
    runTestWriteAction {
      workspaceModel.updateProjectModel { it.addEntity(entity) }
    }

    // then
    listOf(
      dir1,
      dir1Dir2,
      dir1Dir2File,
    ).shouldBeExactlyIncluded()

    listOf(
      file,
      dir1Dir3,
      dir1Dir3File,
      dir4,
      dir4File,
    ).shouldBeExcluded()
  }

  private fun List<VirtualFileUrl>.shouldBeExactlyIncluded() {
    val rootFile = projectRoot.toVirtualFile()
    val actualIncludedFiles = mutableListOf<VirtualFile>()

    runReadAction {
      fileIndex.iterateContentUnderDirectory(rootFile) {
        actualIncludedFiles.add(it)
        true
      }
    }
    val expectedIncludedFiles = this.map { it.toVirtualFile() }

    actualIncludedFiles shouldContainExactlyInAnyOrder expectedIncludedFiles
  }

  private fun List<VirtualFileUrl>.shouldBeExcluded() =
    this.shouldForAll { it.isExcluded() shouldBe true }

  private fun VirtualFileUrl.isExcluded(): Boolean {
    val file = this.toVirtualFile()

    return runReadAction { fileIndexFacade.isExcludedFile(file) }
  }

  private fun VirtualFileUrl.toVirtualFile(): VirtualFile =
    VirtualFileManager.getInstance().refreshAndFindFileByUrl(url)!!
}
