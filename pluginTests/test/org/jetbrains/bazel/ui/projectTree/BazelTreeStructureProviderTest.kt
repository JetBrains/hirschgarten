package org.jetbrains.bazel.ui.projectTree

import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.psi.PsiManager
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.workspace.importer.JAVA_SOURCE_ROOT_TYPE
import org.jetbrains.bazel.workspace.importer.SourceRootBuilder
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.writeText

internal class BazelTreeStructureProviderTest : WorkspaceModelBaseTest() {
  @ParameterizedTest
  @CsvSource(
    "false, false, util",
    "false, true, util",
    "true, false, com.example.mod.util",
    "true, true, com.example.mod.util",
  )
  fun `package names depend on package flattening independently of module flattening`(
    flattenPackages: Boolean,
    flattenModules: Boolean,
    expectedName: String,
  ): Unit = timeoutRunBlocking {
    createSourceRoot()
    readAction {
      val settings = settings(flattenPackages, hideEmptyPackages = false, flattenModules = flattenModules)
      val packageNode = directoryNode("src/com/example/mod/util", settings)
      val parentPath = if (flattenPackages) "src" else "src/com/example/mod"
      packageNode.parent = directoryNode(parentPath, settings)
      assertNodeName(expectedName, packageNode)
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `flattened package names survive early presentation updates`(updateBeforeParent: Boolean): Unit = timeoutRunBlocking {
    createSourceRoot()
    readAction {
      val settings = settings(flattenPackages = true, hideEmptyPackages = false)
      val sourceNode = directoryNode("src", settings)
      val middleNode = directoryNode("src/com/example", settings)
      if (updateBeforeParent) middleNode.update()
      middleNode.parent = sourceNode
      assertNodeName("com.example", middleNode)
      val leafNode = directoryNode("src/com/example/mod/util", settings)
      if (updateBeforeParent) leafNode.update()
      leafNode.parent = sourceNode
      assertNodeName("com.example.mod.util", leafNode)
    }
  }

  @Test
  fun `flattened package names respect abbreviation`(): Unit = timeoutRunBlocking {
    createSourceRoot()
    IndexingTestUtil.suspendUntilIndexesAreReady(project)
    readAction {
      val settings = settings(flattenPackages = true, hideEmptyPackages = false, abbreviatePackages = true)
      val sourceNode = directoryNode("src", settings)
      val packageNode = directoryNode("src/com/example/mod/util", settings)
      packageNode.parent = sourceNode
      assertNodeName("c.e.mod.util", packageNode)
    }
  }

  @Test
  fun `flatten packages preserves merged source roots`(): Unit = timeoutRunBlocking {
    createSourceRoot()
    readAction {
      assertFlattenedPackages(hideEmptyPackages = false)
    }
  }

  @Test
  fun `flatten packages hides empty middle packages`(): Unit = timeoutRunBlocking {
    createSourceRoot()
    readAction {
      assertFlattenedPackages(hideEmptyPackages = true)
    }
  }

  @Test
  fun `packages stay nested when flattening is disabled`(): Unit = timeoutRunBlocking {
    createSourceRoot()
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = false)
      val sourceNode = directoryNode("src", settings)
      assertEquals(listOf("com"), sourceNode.children.filterIsInstance<PsiDirectoryNode>().map { it.value.name })
      val packageNode = directoryNode("src/com/example/mod/util", settings)
      packageNode.parent = directoryNode("src/com/example/mod", settings)
      assertNodeName("util", packageNode)
    }
  }

  @Test
  fun `empty middle packages are compacted without directory package mappings`(): Unit = timeoutRunBlocking {
    createSourceRoot()
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = true)
      val sourceNode = directoryNode("src", settings)
      val packageNode = sourceNode.children.filterIsInstance<PsiDirectoryNode>().single()
      assertEquals("util", packageNode.value.name)
      // directories under a Bazel source root don't correspond to packages
      assertNull(PackageIndex.getInstance(project).getPackageNameByDirectory(packageNode.virtualFile!!))
      val bazelNode = directoryNode("src/com/example/mod/util", settings)
      bazelNode.parent = sourceNode
      assertNodeName("com/example/mod/util", bazelNode)
      assertEquals(listOf("Util.kt"), bazelNode.children.filterIsInstance<PsiFileNode>().map { it.value.name })
    }
  }

  @Test
  fun `flatten packages respects directory filters`(): Unit = timeoutRunBlocking {
    createSourceRoot()
    readAction {
      val settings = settings(flattenPackages = true, hideEmptyPackages = false)
      val sourceNode = directoryNode("src", settings) { it.name != "mod" }
      assertEquals(listOf("com", "example"), sourceNode.children.filterIsInstance<PsiDirectoryNode>().map { it.value.name })
    }
  }

  @Test
  fun `middle directories outside source roots collapse into the source root`(): Unit = timeoutRunBlocking {
    createPackageWithDeepSourceRoot()
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = true)
      val packageNode = directoryNode("common", settings)
      val children = packageNode.children
      val collapsedNode = children.filterIsInstance<PsiDirectoryNode>().single()
      assertEquals(projectBasePath.resolve("common/src/main/java/com/example/common"), collapsedNode.value.virtualFile.toNioPath())
      collapsedNode.parent = packageNode
      assertNodeName("src/main/java/com/example/common", collapsedNode)
      assertEquals(listOf("BUILD.bazel"), children.filterIsInstance<PsiFileNode>().map { it.value.name })
    }
  }

  @Test
  fun `middle directories collapse only into the single child`(): Unit = timeoutRunBlocking {
    createPackageWithDeepSourceRoot()
    projectBasePath.resolve("common/src/test").createDirectories().resolve("README.md").writeText("")
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = true)
      val packageNode = directoryNode("common", settings)
      val srcNode = packageNode.children.filterIsInstance<PsiDirectoryNode>().single()
      srcNode.parent = packageNode
      assertNodeName("src", srcNode)
      val nodes = srcNode.children.filterIsInstance<PsiDirectoryNode>().onEach { it.parent = srcNode }
      assertEquals(
        listOf("main/java/com/example/common", "test"),
        nodes.map { it.update(); it.presentation.presentableText!! }.sorted(),
      )
    }
  }

  @Test
  fun `middle directories collapse into a subdirectory without source roots`(): Unit = timeoutRunBlocking {
    createPackageWithDeepSourceRoot()
    projectBasePath.resolve("common/docs/guide").createDirectories().resolve("README.md").writeText("")
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = true)
      val packageNode = directoryNode("common", settings)
      val nodes = packageNode.children.filterIsInstance<PsiDirectoryNode>().onEach { it.parent = packageNode }
      assertEquals(
        listOf("docs/guide", "src/main/java/com/example/common"),
        nodes.map { it.update(); it.presentation.presentableText!! }.sorted(),
      )
    }
  }

  @Test
  fun `middle directories outside source roots stay nested when compaction is disabled`(): Unit = timeoutRunBlocking {
    createPackageWithDeepSourceRoot()
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = false)
      val packageNode = directoryNode("common", settings)
      val srcNode = packageNode.children.filterIsInstance<PsiDirectoryNode>().single()
      srcNode.parent = packageNode
      assertNodeName("src", srcNode)
    }
  }

  @Test
  fun `middle directories outside bazel packages don't collapse`(): Unit = timeoutRunBlocking {
    createPackageWithDeepSourceRoot(packagePath = "outer/inner/common")
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = true)
      val rootNode = directoryNode(".", settings)
      val outerNode = rootNode.children.filterIsInstance<PsiDirectoryNode>().single { it.value.name == "outer" }
      outerNode.parent = rootNode
      assertNodeName("outer", outerNode)
      val packageNode = directoryNode("outer/inner/common", settings)
      val collapsedNode = packageNode.children.filterIsInstance<PsiDirectoryNode>().single()
      collapsedNode.parent = packageNode
      assertNodeName("src/main/java/com/example/common", collapsedNode)
    }
  }

  @Test
  fun `middle directories inside a bazel package collapse into a nested package`(): Unit = timeoutRunBlocking {
    projectBasePath.resolve("BUILD.bazel").writeText("")
    createPackageWithDeepSourceRoot(packagePath = "outer/inner/common")
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = true)
      val rootNode = directoryNode(".", settings)
      val collapsedNode = rootNode.children.filterIsInstance<PsiDirectoryNode>().single { it.value.name == "common" }
      collapsedNode.parent = rootNode
      assertNodeName("outer/inner/common", collapsedNode)
    }
  }

  @Test
  fun `symlinked middle directories don't collapse`(): Unit = timeoutRunBlocking {
    val testLogs = projectBasePath.resolve("output/testlogs").createDirectories()
    testLogs.resolve("common/package").createDirectories().resolve("test.log").writeText("")
    projectBasePath.resolve("common").createDirectories().resolve("bazel-testlogs").createSymbolicLinkPointingTo(testLogs)
    createPackageWithDeepSourceRoot()
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = true)
      val packageNode = directoryNode("common", settings)
      val nodes = packageNode.children.filterIsInstance<PsiDirectoryNode>().onEach { it.parent = packageNode }
      assertEquals(
        listOf("bazel-testlogs", "src/main/java/com/example/common"),
        nodes.map { it.update(); it.presentation.presentableText!! }.sorted(),
      )
    }
  }

  @Test
  fun `excluded middle directories don't collapse`(): Unit = timeoutRunBlocking {
    projectBasePath.resolve("common/generated/a").createDirectories().resolve("generated.txt").writeText("")
    createPackageWithDeepSourceRoot(excludedPaths = listOf("common/generated"))
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = true)
      val packageNode = directoryNode("common", settings)
      val nodes = packageNode.children.filterIsInstance<PsiDirectoryNode>().onEach { it.parent = packageNode }
      assertEquals(
        listOf("generated", "src/main/java/com/example/common"),
        nodes.map { it.update(); it.presentation.presentableText!! }.sorted(),
      )
    }
  }

  /** The layout which sync produces for a single-file target: the source root is the deepest directory of the sources. */
  private suspend fun createPackageWithDeepSourceRoot(packagePath: String = "common", excludedPaths: List<String> = emptyList()) {
    val packageRoot = projectBasePath.resolve(packagePath).createDirectories()
    packageRoot.resolve("BUILD.bazel").writeText("")
    val sourceRoot = packageRoot.resolve("src/main/java/com/example/common").createDirectories()
    sourceRoot.resolve("Common.java").writeText("package com.example.common; class Common {}")
    VfsUtil.markDirtyAndRefresh(false, true, true, projectBasePath.refreshAndFindVirtualDirectory()!!)

    withContext(Dispatchers.EDT) {
      updateWorkspaceModel { storage ->
        val module = addEmptyJavaModuleEntity("target", storage)
        val roots = listOf(SourceRootBuilder.ResolvedSourceRoot(sourceRoot, generated = false, rootType = JAVA_SOURCE_ROOT_TYPE))
        SourceRootBuilder.write(roots, module, projectBasePath, virtualFileUrlManager, storage)
        // as after sync, the project directories outside source roots are shown in the tree
        val projectRoot = projectBasePath.toVirtualFileUrl(virtualFileUrlManager)
        storage.addEntity(
          BazelProjectDirectoriesEntity(
            projectRoot = projectRoot,
            includedRoots = listOf(projectRoot),
            excludedRoots = excludedPaths.map { projectBasePath.resolve(it).toVirtualFileUrl(virtualFileUrlManager) },
            indexAllFilesInIncludedRoots = false,
            indexAdditionalFiles = emptyList(),
            entitySource = BazelProjectEntitySource,
          ),
        )
      }
    }
  }

  private suspend fun createSourceRoot() {
    val sourceRoot = projectBasePath.resolve("src").createDirectories()
    sourceRoot.resolve("Root.java").writeText("class Root {}")
    sourceRoot.resolve("com/example/mod/util").createDirectories().resolve("Util.kt").writeText("package com.example.mod.util\nobject Util")
    sourceRoot.refreshAndFindVirtualDirectory()!!

    withContext(Dispatchers.EDT) {
      updateWorkspaceModel { storage ->
        val module = addEmptyJavaModuleEntity("target", storage)
        val roots = listOf(SourceRootBuilder.ResolvedSourceRoot(sourceRoot, generated = false, rootType = JAVA_SOURCE_ROOT_TYPE))
        SourceRootBuilder.write(roots, module, projectBasePath, virtualFileUrlManager, storage)
      }
    }
  }

  private fun assertFlattenedPackages(hideEmptyPackages: Boolean) {
    val settings = settings(flattenPackages = true, hideEmptyPackages)
    val sourceNode = directoryNode("src", settings)
    val children = sourceNode.children
    val packages = children.filterIsInstance<PsiDirectoryNode>()
    val expected = if (hideEmptyPackages) listOf("com/example/mod/util")
    else listOf("com", "com/example", "com/example/mod", "com/example/mod/util")
    val sourcePath = projectBasePath.resolve("src")
    assertEquals(expected, packages.map { sourcePath.relativize(it.value.virtualFile.toNioPath()).invariantSeparatorsPathString }.sorted())
    for (node in packages) {
      node.parent = sourceNode
      val expectedName = sourcePath.relativize(node.value.virtualFile.toNioPath()).invariantSeparatorsPathString.replace('/', '.')
      assertNodeName(expectedName, node)
    }
    assertNodeName("src", sourceNode)
    assertEquals(listOf("Root.java"), children.filterIsInstance<PsiFileNode>().map { it.value.name })
    val utilNode = directoryNode("src/com/example/mod/util", settings)
    assertEquals(listOf("Util.kt"), utilNode.children.filterIsInstance<PsiFileNode>().map { it.value.name })
    assertEquals(emptyList<PsiDirectoryNode>(), utilNode.children.filterIsInstance<PsiDirectoryNode>())
  }

  private fun assertNodeName(expected: String, node: PsiDirectoryNode) {
    node.update()
    val presentation = node.presentation
    assertEquals(expected, presentation.presentableText)
    val displayedName = if (presentation.coloredText.isEmpty()) presentation.presentableText
    else presentation.coloredText.joinToString("") { it.text }.trimEnd()
    assertEquals(expected, displayedName)
  }

  private fun directoryNode(
    relativePath: String,
    settings: ViewSettings,
    filter: PsiFileSystemItemFilter? = null,
  ): PsiDirectoryNode {
    val directory = PsiManager.getInstance(project).findDirectory(
      projectBasePath.resolve(relativePath).refreshAndFindVirtualDirectory()!!,
    )!!
    val node = PsiDirectoryNode(project, directory, settings, filter)
    return BazelTreeStructureProvider().modify(node, listOf(node), settings).single() as PsiDirectoryNode
  }

  private fun settings(
    flattenPackages: Boolean,
    hideEmptyPackages: Boolean,
    abbreviatePackages: Boolean = false,
    flattenModules: Boolean = false,
  ): ViewSettings = object : ProjectViewSettings {
    override fun isFlattenPackages(): Boolean = flattenPackages

    override fun isFlattenModules(): Boolean = flattenModules

    override fun isHideEmptyMiddlePackages(): Boolean = hideEmptyPackages

    override fun isAbbreviatePackageNames(): Boolean = abbreviatePackages

    override fun isShowExcludedFiles(): Boolean = true
  }
}
