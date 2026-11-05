/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.golang.treeview

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.entities
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.golang.sync.GO_EXTERNAL_LIBRARY_ROOT_NAME
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity
import java.nio.file.Path
import java.nio.file.Paths
import java.util.SortedMap
import java.util.TreeMap

/**
 * Modifies the project view by replacing the External Go Libraries root node (containing a flat
 * list of sources) with a root node that structures sources based on their import paths.
 */
@ApiStatus.Internal
class BazelGoTreeStructureProvider :
  TreeStructureProvider,
  DumbAware {
  override fun modify(
    parent: AbstractTreeNode<*>,
    children: Collection<AbstractTreeNode<*>>,
    settings: ViewSettings,
  ): Collection<AbstractTreeNode<*>?> {
    val project: Project = parent.project
    if (!project.isBazelProject ||
      !isGoBazelExternalLibraryRoot(parent)
    ) {
      return children
    }
    val originalFileNodes = getOriginalFileNodesMap(children)

    val newChildren = TreeMap<String, AbstractTreeNode<*>>()

    for (goPackage in parent.project.workspaceModel.currentSnapshot.entities<BazelGoPackageEntity>()) {
      if (goPackage.importPath.isEmpty()) {
        continue
      }
      generateTree(
        settings,
        project,
        newChildren,
        goPackage.importPath,
        goPackage.sources.mapNotNull { it.virtualFile },
        originalFileNodes,
      )
    }
    return newChildren.values
  }
}

private fun generateTree(
  settings: ViewSettings,
  project: Project,
  newChildren: SortedMap<String, AbstractTreeNode<*>>,
  importPath: String,
  availableFiles: Collection<VirtualFile>,
  originalFileNodes: Map<VirtualFile, AbstractTreeNode<*>>,
) {
  val pathIter: Iterator<Path> = Paths.get(importPath).iterator()
  if (!pathIter.hasNext()) {
    return
  }
  // Root nodes (e.g., src) have to be added directly to newChildren.
  val rootName: String = pathIter.next().toString()
  val root: GoSyntheticLibraryElementNode =
    newChildren.computeIfAbsent(
      rootName,
    ) { unused ->
      GoSyntheticLibraryElementNode(
        project,
        BazelGoExternalSyntheticLibrary(rootName, availableFiles),
        rootName,
        settings,
        TreeMap(),
      )
    } as GoSyntheticLibraryElementNode

  // Child nodes (e.g., package_name under src) are added under root nodes recursively.
  val leaf: GoSyntheticLibraryElementNode =
    buildChildTree(settings, project, availableFiles, pathIter, root)

  // Required for files to actually show up in the Project View.
  availableFiles.forEach { file -> originalFileNodes[file]?.also { leaf.addChild(file.name, it) } }
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
  files: Collection<VirtualFile>,
  pathIter: Iterator<Path>,
  parent: GoSyntheticLibraryElementNode,
): GoSyntheticLibraryElementNode {
  var parent: GoSyntheticLibraryElementNode = parent
  while (pathIter.hasNext()) {
    parent.addFiles(files)
    val dirName: String = pathIter.next().toString()

    // current path already was created, no need to re-create
    if (parent.hasChild(dirName)) {
      parent = parent.getChildNode(dirName) ?: continue
      continue
    }
    val libraryNode =
      GoSyntheticLibraryElementNode(
        project,
        BazelGoExternalSyntheticLibrary(dirName, files),
        dirName,
        settings,
        TreeMap(),
      )
    parent.addChild(dirName, libraryNode)
    parent = libraryNode
  }
  return parent
}

private fun isGoBazelExternalLibraryRoot(parent: AbstractTreeNode<*>): Boolean {
  if (parent.name == null) {
    return false
  }
  return parent.name.equals(GO_EXTERNAL_LIBRARY_ROOT_NAME)
}

private fun getOriginalFileNodesMap(children: Collection<AbstractTreeNode<*>>): Map<VirtualFile, AbstractTreeNode<*>> =
  children
    .asSequence()
    .filterIsInstance<PsiFileNode>()
    .mapNotNull { node -> node.virtualFile?.let { it to node } }
    .toMap()
