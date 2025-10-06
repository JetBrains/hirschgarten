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

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import java.util.SortedSet
import java.util.TreeSet
import javax.swing.Icon

/** Represents a [SyntheticLibrary] with a mutable set of child files.  */
class BazelGoExternalSyntheticLibrary(private val presentableText: String, childFiles: Set<VirtualFile>) :
  SyntheticLibrary(),
  ItemPresentation {
  private val childFiles: SortedSet<VirtualFile> = TreeSet(Comparator.comparing(VirtualFile::toString)).also { it.addAll(childFiles) }

  fun addFiles(files: Set<VirtualFile>) {
    childFiles.addAll(files)
  }

  override fun getIcon(unused: Boolean): Icon = BazelPluginIcons.bazel

  override fun getSourceRoots(): SortedSet<VirtualFile> = childFiles

  override fun equals(o: Any?): Boolean =
    o is BazelGoExternalSyntheticLibrary &&
      o.presentableText == presentableText &&
      o.sourceRoots == sourceRoots

  override fun hashCode(): Int = presentableText.hashCode()

  override fun getPresentableText(): String = presentableText
}
