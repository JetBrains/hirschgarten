/*
 * Copyright 2022 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.golang.treeview

import com.google.common.collect.ImmutableList
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.projectView.impl.nodes.SyntheticLibraryElementNode
import com.intellij.lang.FileASTNode
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.search.PsiElementProcessor
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.golang.resolve.BazelGoPackageFactory.Companion.fileToImportPathMapComputable
import org.jetbrains.bazel.golang.sync.GO_EXTERNAL_LIBRARY_ROOT_NAME
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.sync.libraries.BazelExternalSyntheticLibrary
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

/** Unit tests for [BazelGoTreeStructureProvider] */
class BazelGoTreeStructureProviderTest : MockProjectBaseTest() {
  private lateinit var fileToImportPathMap: ConcurrentHashMap<Path, String>
  private lateinit var rootNode: SyntheticLibraryElementNode

  @BeforeEach
  fun beforeEach() {
    project.isBazelProject = true
    rootNode = createRootNode(GO_EXTERNAL_LIBRARY_ROOT_NAME)
    fileToImportPathMap = ConcurrentHashMap<Path, String>()
    SyncCache.getInstance(project).clear()
  }

  @Test
  fun givenDifferentImportPaths_createTwoRootNodes() {
    // GIVEN

    val fileName1 = "bar.go"
    val file1 = MockVirtualFile.file(fileName1)
    val fileNode1 = createPsiFileNode(file1)
    fileToImportPathMap[Path(file1.path)] = "root1"

    val fileName2 = "buzz.go"
    val file2 = MockVirtualFile.file(fileName2)
    val fileNode2 = createPsiFileNode(file2)
    fileToImportPathMap[Path(file2.path)] = "root2"

    SyncCache.getInstance(project).injectValueForTest(fileToImportPathMapComputable, fileToImportPathMap)

    // WHEN
    val actualChildren =
      BazelGoTreeStructureProvider()
        .modify(
          // parent=
          rootNode,
          // children=
          ImmutableList.of(fileNode1, fileNode2),
          // settings=
          object : ViewSettings {},
        ).map { it as GoSyntheticLibraryElementNode }

    // THEN
    actualChildren shouldHaveSize 2
    actualChildren[0].name shouldBe "root1"
    actualChildren[1].name shouldBe "root2"

    val secondLevelImportPath1 = actualChildren[0].children.toList()
    assert(secondLevelImportPath1.size == 1)
    assert(secondLevelImportPath1.first() == fileNode1)

    val secondLevelImportPath2 = actualChildren[1].children.toList()
    assert(secondLevelImportPath2.size == 1)
    assert(secondLevelImportPath2.first() == fileNode2)
  }

  private fun createPsiFileNode(virtualFile: VirtualFile): PsiFileNode =
    PsiFileNode(
      project,
      FakePsiFile(project, virtualFile),
      object : ViewSettings {},
    )

  private fun createRootNode(nodeName: String): SyntheticLibraryElementNode {
    val parentLibrary =
      BazelExternalSyntheticLibrary(
        nodeName,
        ImmutableList.of(),
      )

    return SyntheticLibraryElementNode(
      project,
      parentLibrary,
      parentLibrary,
      object : ViewSettings {},
    )
  }

  private abstract class FakePsiFileSystemItem(private val project: Project, private val vf: VirtualFile) :
    FakePsiElement(),
    PsiFileSystemItem {
    override fun getName(): String = name

    override fun getVirtualFile(): VirtualFile = vf

    override fun getProject(): Project = project

    override fun toString(): String = vf.path

    override fun isValid(): Boolean = true

    override fun canNavigate(): Boolean = false

    override fun canNavigateToSource(): Boolean = false

    override fun navigate(requestFocus: Boolean) {}

    override fun processChildren(processor: PsiElementProcessor<in PsiFileSystemItem>): Boolean = false

    override fun checkSetName(name: String) {}
  }

  private class FakePsiFile(private val project: Project, private val virtualFile: VirtualFile) :
    FakePsiFileSystemItem(project, virtualFile),
    PsiFile {
    override fun getNode(): FileASTNode? = null

    override fun getParent(): PsiDirectory? = null

    override fun getContainingFile(): PsiFile = this

    override fun getContainingDirectory(): PsiDirectory? = null

    override fun isDirectory(): Boolean = false

    override fun getModificationStamp(): Long = 0

    override fun getOriginalFile(): PsiFile = this

    override fun getFileType(): FileType = FileTypeManager.getInstance().getFileTypeByFileName(virtualFile.name)

    override fun getPsiRoots(): Array<PsiFile> = PsiFile.EMPTY_ARRAY

    override fun getViewProvider(): FileViewProvider = PsiManager.getInstance(project).findViewProvider(virtualFile)!!

    override fun subtreeChanged() {}
  }
}
