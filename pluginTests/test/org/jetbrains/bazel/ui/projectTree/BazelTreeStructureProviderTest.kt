package org.jetbrains.bazel.ui.projectTree

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.psi.PsiManager
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.RegistryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap
import org.jetbrains.bazel.workspace.importer.DummyModuleSplitter
import org.jetbrains.bazel.workspace.importer.JAVA_SOURCE_ROOT_TYPE
import org.jetbrains.bazel.workspace.importer.PackageMarkerBuilder
import org.jetbrains.bazel.workspace.importer.SourceRootBuilder
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.writeText

internal class BazelTreeStructureProviderTest : WorkspaceModelBaseTest() {
  @ParameterizedTest
  @CsvSource(
    "false, false, util",
    "false, true, util",
    "true, false, org.example.com.example.mod.util",
    "true, true, org.example.com.example.mod.util",
  )
  @RegistryKey(key = "bazel.merge.source.roots", value = "false")
  fun `package names depend on package flattening independently of module flattening`(
    flattenPackages: Boolean,
    flattenModules: Boolean,
    expectedName: String,
  ): Unit = timeoutRunBlocking {
    createSourceRoots(merged = false, packagePrefix = "org.example")
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
  @RegistryKey(key = "bazel.merge.source.roots", value = "false")
  fun `flattened package names survive early presentation updates`(updateBeforeParent: Boolean): Unit = timeoutRunBlocking {
    createSourceRoots(merged = false)
    readAction {
      val settings = settings(flattenPackages = true, hideEmptyPackages = false)
      val sourceNode = directoryNode("src", settings)
      val markerNode = directoryNode("src/com/example", settings)
      if (updateBeforeParent) markerNode.update()
      markerNode.parent = sourceNode
      assertTrue(ProjectRootsUtil.isSourceRoot(markerNode.value))
      assertNodeName("com.example", markerNode)
      val contentRootNode = directoryNode("src/com/example/mod/util", settings)
      if (updateBeforeParent) contentRootNode.update()
      contentRootNode.parent = sourceNode
      assertTrue(ProjectRootsUtil.isModuleContentRoot(contentRootNode.value))
      assertNodeName("com.example.mod.util", contentRootNode)
    }
  }

  @Test
  @RegistryKey(key = "bazel.merge.source.roots", value = "false")
  fun `flattened names include the source root package prefix`(): Unit = timeoutRunBlocking {
    createSourceRoots(merged = false, packagePrefix = "org.example")
    readAction {
      assertFlattenedPackages(hideEmptyPackages = false, packagePrefix = "org.example")
    }
  }

  @Test
  @RegistryKey(key = "bazel.merge.source.roots", value = "false")
  fun `flattened package names respect abbreviation`(): Unit = timeoutRunBlocking {
    createSourceRoots(merged = false)
    IndexingTestUtil.suspendUntilIndexesAreReady(project)
    readAction {
      val settings = settings(flattenPackages = true, hideEmptyPackages = false, abbreviatePackages = true)
      val sourceNode = directoryNode("src", settings)
      val packageNode = directoryNode("src/com/example/mod/util", settings)
      packageNode.parent = sourceNode
      assertNodeName("c.e.mod.util", packageNode)
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  @RegistryKey(key = "bazel.merge.source.roots", value = "false")
  fun `flatten packages crosses dummy module boundaries`(hideEmptyPackages: Boolean): Unit = timeoutRunBlocking {
    createSourceRoots(merged = false)

    readAction {
      val sourceDirectory = projectBasePath.resolve("src").refreshAndFindVirtualDirectory()!!
      val fileIndex = ProjectFileIndex.getInstance(project)
      assertNotEquals(
        fileIndex.getModuleForFile(sourceDirectory),
        fileIndex.getModuleForFile(sourceDirectory.findChild("com")!!),
      )
      assertFlattenedPackages(hideEmptyPackages)
    }
  }

  @Test
  @RegistryKey(key = "bazel.merge.source.roots", value = "true")
  fun `flatten packages preserves merged source roots`(): Unit = timeoutRunBlocking {
    createSourceRoots(merged = true)
    readAction {
      assertFlattenedPackages(hideEmptyPackages = false)
    }
  }

  @Test
  @RegistryKey(key = "bazel.merge.source.roots", value = "false")
  fun `packages stay nested when flattening is disabled`(): Unit = timeoutRunBlocking {
    createSourceRoots(merged = false)
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = false)
      val sourceNode = directoryNode("src", settings)
      assertEquals(listOf("com"), sourceNode.children.filterIsInstance<PsiDirectoryNode>().map { it.value.name })
      val packageNode = directoryNode("src/com/example/mod/util", settings)
      packageNode.parent = directoryNode("src/com/example/mod", settings)
      assertNodeName("util", packageNode)
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  @RegistryKey(key = "bazel.merge.source.roots", value = "false")
  fun `empty middle packages are compacted without directory package mappings`(flattenPackages: Boolean): Unit = timeoutRunBlocking {
    createSourceRoots(merged = false)
    readAction {
      val settings = settings(flattenPackages, hideEmptyPackages = true)
      val sourceNode = directoryNode("src", settings)
      assertNull(PackageIndex.getInstance(project).getPackageNameByDirectory(sourceNode.virtualFile!!))
      val packageNode = sourceNode.children.filterIsInstance<PsiDirectoryNode>().single()
      assertEquals("util", packageNode.value.name)
      assertNull(PackageIndex.getInstance(project).getPackageNameByDirectory(packageNode.virtualFile!!))
      val bazelNode = directoryNode("src/com/example/mod/util", settings)
      bazelNode.parent = sourceNode
      assertNodeName("com.example.mod.util", bazelNode)
      assertEquals(listOf("Util.kt"), bazelNode.children.filterIsInstance<PsiFileNode>().map { it.value.name })
    }
  }

  @Test
  @RegistryKey(key = "bazel.merge.source.roots", value = "true")
  fun `empty middle packages are compacted under merged source roots`(): Unit = timeoutRunBlocking {
    createSourceRoots(merged = true)
    readAction {
      val settings = settings(flattenPackages = false, hideEmptyPackages = true)
      val sourceNode = directoryNode("src", settings)
      val packageNode = sourceNode.children.filterIsInstance<PsiDirectoryNode>().single()
      assertEquals("util", packageNode.value.name)
      val bazelNode = directoryNode("src/com/example/mod/util", settings)
      bazelNode.parent = sourceNode
      assertNodeName("com.example.mod.util", bazelNode)
    }
  }

  @Test
  @RegistryKey(key = "bazel.merge.source.roots", value = "false")
  fun `flatten packages respects directory filters`(): Unit = timeoutRunBlocking {
    createSourceRoots(merged = false)
    readAction {
      val settings = settings(flattenPackages = true, hideEmptyPackages = false)
      val sourceNode = directoryNode("src", settings) { it.name != "mod" }
      assertEquals(listOf("com", "example"), sourceNode.children.filterIsInstance<PsiDirectoryNode>().map { it.value.name })
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  @RegistryKey(key = "bazel.merge.source.roots", value = "false")
  fun `flattened names use nested source packages instead of an ancestor file package`(hideEmptyPackages: Boolean): Unit = timeoutRunBlocking {
    val sourceRoot = projectBasePath.resolve("src/mod-impl/src").createDirectories()
    val rootSource = sourceRoot.resolve("ModImpl.java").apply { writeText("package com.example.mod.impl; class ModImpl {}") }
    val contextSource = sourceRoot.resolve("context.kt").apply { writeText("package com.example.mod.impl\nobject Context") }
    val utilSource = sourceRoot.resolve("com/example/mod/util").createDirectories().resolve("Util.kt").apply {
      writeText("package com.example.mod.util\nobject Util")
    }
    val excludedDirectory = sourceRoot.resolve("com/example/mod/somethingelse").createDirectories()
    excludedDirectory.resolve("Something.kt").writeText("package com.example.mod.somethingelse\nobject Something")
    sourceRoot.refreshAndFindVirtualDirectory()!!

    val resolver = DefaultJvmPackageResolver()
    val roots = listOf(rootSource, contextSource, utilSource).map {
      SourceRootBuilder.ResolvedSourceRoot(it, false, resolver.calculateJvmPackagePrefix(it)!!, JAVA_SOURCE_ROOT_TYPE)
    }
    val split = DummyModuleSplitter(projectBasePath, FileToTargetMap.EMPTY).split(sourceRoot.parent, roots)
                as DummyModuleSplitter.DummyModulesToAdd
    withContext(Dispatchers.EDT) {
      updateWorkspaceModel { storage ->
        val module = addEmptyJavaModuleEntity("target", storage)
        SourceRootBuilder.write(roots, module, projectBasePath, virtualFileUrlManager, storage)
        val markers = PackageMarkerBuilder(roots.map { it.sourcePath }.toSet(), setOf(excludedDirectory), roots)
        for (dummy in split.dummies) {
          markers.write(dummy.sourceRoot, addEmptyJavaModuleEntity(dummy.name, storage), virtualFileUrlManager, storage)
        }
      }
    }

    readAction {
      val settings = settings(flattenPackages = true, hideEmptyPackages)
      val sourceNode = directoryNode("src/mod-impl/src", settings) { it.name != "somethingelse" }
      val packages = sourceNode.children.filterIsInstance<PsiDirectoryNode>()
      val expected = if (hideEmptyPackages) listOf("com.example.mod.util")
      else listOf("com", "com.example", "com.example.mod", "com.example.mod.util")
      assertEquals(expected.size, packages.size)
      for (node in packages) {
        node.parent = sourceNode
        val relativeName = sourceRoot.relativize(node.value.virtualFile.toNioPath()).invariantSeparatorsPathString.replace('/', '.')
        assertNodeName(relativeName, node)
      }
      assertEquals(expected, packages.map { it.presentation.presentableText!! }.sorted())
    }
  }

  private suspend fun createSourceRoots(merged: Boolean, packagePrefix: String = "") {
    val sourceRoot = projectBasePath.resolve("src").createDirectories()
    val rootSource = sourceRoot.resolve("Root.java").apply {
      val packageStatement = if (packagePrefix.isEmpty()) "" else "package $packagePrefix;\n"
      writeText("${packageStatement}class Root {}")
    }
    val utilPackage = if (packagePrefix.isEmpty()) "com.example.mod.util" else "$packagePrefix.com.example.mod.util"
    val utilSource = sourceRoot.resolve("com/example/mod/util").createDirectories().resolve("Util.kt").apply {
      writeText("package $utilPackage\nobject Util")
    }
    sourceRoot.refreshAndFindVirtualDirectory()!!

    withContext(Dispatchers.EDT) {
      updateWorkspaceModel { storage ->
        val module = addEmptyJavaModuleEntity("target", storage)
        val roots = if (merged) {
          listOf(SourceRootBuilder.ResolvedSourceRoot(sourceRoot, false, packagePrefix, JAVA_SOURCE_ROOT_TYPE))
        }
        else {
          listOf(
            SourceRootBuilder.ResolvedSourceRoot(rootSource, false, packagePrefix, JAVA_SOURCE_ROOT_TYPE),
            SourceRootBuilder.ResolvedSourceRoot(utilSource, false, utilPackage, JAVA_SOURCE_ROOT_TYPE),
          )
        }
        SourceRootBuilder.write(roots, module, projectBasePath, virtualFileUrlManager, storage)
        if (!merged) {
          val dummyModule = addEmptyJavaModuleEntity("dummy", storage)
          PackageMarkerBuilder(emptySet(), emptySet(), roots).write(
            SourceRootBuilder.ResolvedSourceRoot(sourceRoot, false, packagePrefix, JAVA_SOURCE_ROOT_TYPE),
            dummyModule,
            virtualFileUrlManager,
            storage,
          )
        }
      }
    }
  }

  private fun assertFlattenedPackages(hideEmptyPackages: Boolean, packagePrefix: String = "") {
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
      val relativeName = sourcePath.relativize(node.value.virtualFile.toNioPath()).invariantSeparatorsPathString.replace('/', '.')
      val expectedName = if (packagePrefix.isEmpty()) relativeName else "$packagePrefix.$relativeName"
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
  ): ViewSettings = object : ViewSettings {
    override fun isFlattenPackages(): Boolean = flattenPackages

    override fun isFlattenModules(): Boolean = flattenModules

    override fun isHideEmptyMiddlePackages(): Boolean = hideEmptyPackages

    override fun isAbbreviatePackageNames(): Boolean = abbreviatePackages
  }
}
