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

import com.goide.GoIcons
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.SyntheticLibraryElementNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import java.util.SortedMap

/**
 * Represents a Go directory node within the "External Libraries" project view.
 *
 *
 * It can have an arbitrary amount of child nodes, forming a tree. Child nodes may be added and
 * queried with the appropriate methods.
 */
internal class GoSyntheticLibraryElementNode(
  project: Project,
  library: BlazeGoExternalSyntheticLibrary,
  dirName: String,
  settings: ViewSettings,
  private val children: SortedMap<String, AbstractTreeNode<*>>
) : SyntheticLibraryElementNode(
  project,
  library,
  BlazeGoLibraryItemPresentation(dirName),
  settings,
) {

  override fun getChildren(): MutableCollection<AbstractTreeNode<*>?> {
    return children.values
  }

  fun getChildNode(dirName: String?): GoSyntheticLibraryElementNode? {
    return children[dirName] as? GoSyntheticLibraryElementNode
  }

  fun addChild(dirName: String?, child: AbstractTreeNode<*>?) {
    children.put(dirName, child)
  }

  fun hasChild(dirName: String?): Boolean {
    return children.containsKey(dirName)
  }

  fun addFiles(files: Set<VirtualFile>) {
    (getValue() as BlazeGoExternalSyntheticLibrary).addFiles(files)
  }

  private class BlazeGoLibraryItemPresentation(val text: String) : ItemPresentation {
    override fun getPresentableText(): @NlsSafe String? {
      return text
    }

    override fun getIcon(ignore: Boolean): javax.swing.Icon? {
      return GoIcons.PACKAGE
    }
  }
}
