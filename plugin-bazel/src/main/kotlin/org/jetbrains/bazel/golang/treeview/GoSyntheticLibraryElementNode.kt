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
package org.jetbrains.bazel.golang.treeview

import com.goide.GoIcons
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.SyntheticLibraryElementNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.SortedMap
import javax.swing.Icon

/**
 * Represents a Go directory node within the "External Libraries" project view.
 *
 *
 * It can have an arbitrary amount of child nodes, forming a tree. Child nodes may be added and
 * queried with the appropriate methods.
 */
class GoSyntheticLibraryElementNode(
  project: Project,
  library: BazelGoExternalSyntheticLibrary,
  dirName: String,
  settings: ViewSettings,
  children: SortedMap<String, AbstractTreeNode<*>>,
) : SyntheticLibraryElementNode(project, library, BazelGoLibraryItemPresentation(dirName), settings) {
  private val children: MutableMap<String, AbstractTreeNode<*>> = children

  override fun getChildren(): Collection<AbstractTreeNode<*>> = children.values

  fun getChildNode(dirName: String): GoSyntheticLibraryElementNode? = children[dirName] as? GoSyntheticLibraryElementNode

  fun addChild(dirName: String, child: AbstractTreeNode<*>) {
    children[dirName] = child
  }

  fun hasChild(dirName: String): Boolean = children.containsKey(dirName)

  fun addFiles(files: Set<VirtualFile>) {
    (value as? BazelGoExternalSyntheticLibrary)?.addFiles(files)
  }

  private class BazelGoLibraryItemPresentation(val myPresentableText: String) : ItemPresentation {
    override fun getPresentableText(): String = myPresentableText

    override fun getIcon(ignore: Boolean): Icon = GoIcons.PACKAGE
  }
}
