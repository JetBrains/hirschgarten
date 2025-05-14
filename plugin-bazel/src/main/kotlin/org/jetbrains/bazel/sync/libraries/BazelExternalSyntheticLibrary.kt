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
package org.jetbrains.bazel.sync.libraries

import com.google.common.collect.ImmutableSet.toImmutableSet
import com.google.common.collect.Sets
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.utils.VfsUtils
import java.nio.file.Path
import java.util.Objects
import java.util.stream.Collectors.toSet
import javax.swing.Icon

/**
 * A [SyntheticLibrary] pointing to a list of external files for a language. Only supports one
 * instance per value of presentableText.
 */
class BazelExternalSyntheticLibrary(private val presentableText: String, files: List<Path>) :
  SyntheticLibrary(),
  ItemPresentation {
  private val files: Set<Path> = files.toSet()
  private val validFiles: MutableSet<VirtualFile> =
    Sets.newConcurrentHashSet(
      files
        .stream()
        .map { VfsUtils.resolveVirtualFile(it.toFile(), true) }
        .filter(Objects::nonNull)
        .collect(toSet()),
    )

  override fun getPresentableText(): String = presentableText

  fun removeInvalidFiles(deletedFiles: Set<VirtualFile>) {
    if (deletedFiles.stream().anyMatch(VirtualFile::isDirectory)) {
      validFiles.removeIf { f -> !f.isValid }
    } else {
      validFiles.removeAll(deletedFiles)
    }
  }

  private fun restoreMissingFiles() {
    if (validFiles.size < files.size) {
      Sets
        .difference(
          files,
          validFiles
            .stream()
            .filter(VirtualFile::isValid)
            .map(VfsUtil::virtualToIoFile)
            .collect(toImmutableSet()),
        ).stream()
        .map { file -> VfsUtils.resolveVirtualFile(file.toFile(), false) }
        .filter(Objects::nonNull)
        .forEach { it?.also { validFiles.add(it) } }
    }
  }

  override fun getSourceRoots(): MutableCollection<VirtualFile> {
    // this must return a set, otherwise SyntheticLibrary#contains will create a new set each time
    // it's invoked (very frequently, on the EDT)
    return validFiles
  }

  override fun equals(other: Any?): Boolean {
    // intended to be only a single instance added to the project for each value of presentableText
    return other is BazelExternalSyntheticLibrary &&
      presentableText == other.presentableText
  }

  override fun hashCode(): Int = presentableText.hashCode()

  override fun getLocationString(): String? = null

  override fun getIcon(unused: Boolean): Icon = BazelPluginIcons.bazel
}
