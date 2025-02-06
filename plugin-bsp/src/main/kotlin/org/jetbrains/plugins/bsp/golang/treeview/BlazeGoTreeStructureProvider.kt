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
package org.jetbrains.plugins.bsp.golang.treeview

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MultiMap
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.golang.resolve.BlazeGoPackageFactory
import org.jetbrains.plugins.bsp.golang.resolve.GO_EXTERNAL_LIBRARY_ROOT_NAME
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.SortedMap
import java.util.TreeMap


/**
 * Modifies the project view by replacing the External Go Libraries root node (containing a flat
 * list of sources) with a root node that structures sources based on their import paths.
 */
class BlazeGoTreeStructureProvider : TreeStructureProvider, DumbAware {
  override fun modify(
    parent: AbstractTreeNode<*>,
    children: MutableCollection<AbstractTreeNode<*>>,
    settings: ViewSettings
  ): MutableCollection<AbstractTreeNode<*>> {
    val project = parent.project
    if (!project.isBspProject || !isGoBlazeExternalLibraryRoot(parent)) {
      return children
    }
    val originalFileNodes = getOriginalFileNodesMap(children)

    val fileToImportPathMap: MutableMap<File, String>? =
      BlazeGoPackageFactory.getFileToImportPathMap(project)
    if (fileToImportPathMap == null) {
      return children
    }

    val newChildren = TreeMap<String, AbstractTreeNode<*>>()

    val importPathToFilesMap = MultiMap<String, VirtualFile>()
    originalFileNodes.keys.forEach { file ->
      fileToImportPathMap.getOrDefault(VfsUtil.virtualToIoFile(file), "").let { importPathToFilesMap.putValue(it, file) }
    }

    for (importPath in importPathToFilesMap.keySet()) {
      if (importPath.isEmpty()) {
        continue
      }
      generateTree(
        settings,
        project,
        newChildren,
        importPath,
        importPathToFilesMap.get(importPath).toSet(),
        originalFileNodes,
      )
    }
    return (newChildren.values + importPathToFilesMap.get("").mapNotNull { key: VirtualFile -> originalFileNodes[key] }).toMutableList()
  }

  companion object {
    private fun generateTree(
      settings: ViewSettings,
      project: Project,
      newChildren: SortedMap<String, AbstractTreeNode<*>>,
      importPath: String,
      availableFiles: Set<VirtualFile>,
      originalFileNodes: Map<VirtualFile, AbstractTreeNode<*>>
    ) {
      val pathIter = Paths.get(importPath).iterator()
      if (!pathIter.hasNext()) {
        return
      }
      // Root nodes (e.g., src) have to be added directly to newChildren.
      val rootName = pathIter.next().toString()
      val root: GoSyntheticLibraryElementNode =
        newChildren.computeIfAbsent(
          rootName,
        ) { _ ->
          GoSyntheticLibraryElementNode(
            project,
            BlazeGoExternalSyntheticLibrary(rootName, availableFiles),
            rootName,
            settings,
            TreeMap(),
          )
        } as GoSyntheticLibraryElementNode

      // Child nodes (e.g., package_name under src) are added under root nodes recursively.
      val leaf: GoSyntheticLibraryElementNode = buildChildTree(
        settings,
        project,
        availableFiles,
        pathIter,
        root,
      )

      // Required for files to actually show up in the Project View.
      availableFiles.forEach { file ->
        leaf.addChild(file.name, originalFileNodes[file])
      }
    }

    /**
     * Recurse down the import path tree and add elements as children.
     *
     *
     * Fills previously created nodes with source files from the current import path.
     */
    private fun buildChildTree(
      settings: ViewSettings,
      project: Project,
      files: Set<VirtualFile>,
      pathIter: Iterator<Path>,
      parent: GoSyntheticLibraryElementNode
    ): GoSyntheticLibraryElementNode {
      var parent: GoSyntheticLibraryElementNode = parent
      while (pathIter.hasNext()) {
        parent.addFiles(files)
        val dirName = pathIter.next().toString()

        // current path already was created, no need to re-create
        if (parent.hasChild(dirName)) {
          parent = parent.getChildNode(dirName) ?: return parent
          continue
        }
        val libraryNode =
          GoSyntheticLibraryElementNode(
            project,
            BlazeGoExternalSyntheticLibrary(dirName, files),
            dirName,
            settings,
            TreeMap(),
          )
        parent.addChild(dirName, libraryNode)
        parent = libraryNode
      }
      return parent
    }

    private fun isGoBlazeExternalLibraryRoot(parent: AbstractTreeNode<*>): Boolean {
      if (parent.name == null) {
        return false
      }
      return parent.name == GO_EXTERNAL_LIBRARY_ROOT_NAME
    }

    private fun getOriginalFileNodesMap(
      children: MutableCollection<AbstractTreeNode<*>>
    ): Map<VirtualFile, AbstractTreeNode<*>> {
      return children
        .mapNotNull { (it as? PsiFileNode)?.virtualFile?.let { file -> file to it } }
        .toMap()

    }
  }
}
