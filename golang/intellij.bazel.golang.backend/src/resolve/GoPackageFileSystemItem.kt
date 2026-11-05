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
package org.jetbrains.bazel.golang.resolve

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.SyntheticFileSystemItem
import com.intellij.psi.search.PsiElementProcessor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants

/** Presents a target in a BUILD file as a file system item, because an import reference resolves only to file system items. */
@ApiStatus.Internal
class GoPackageFileSystemItem private constructor(private val name: String, private val rule: PsiElement) :
  SyntheticFileSystemItem(rule.project) {
  companion object {
    /** Returns [element] itself if it is a file system item, or a wrapper for a named target. */
    fun getInstance(element: PsiElement): PsiFileSystemItem? =
      when (element) {
        is PsiFileSystemItem -> element
        is PsiNamedElement -> element.name?.let { GoPackageFileSystemItem(it, element) }
        else -> null
      }
  }

  override fun getName(): String = name

  override fun getNavigationElement(): PsiElement = rule

  override fun getParent(): PsiFileSystemItem? = rule.containingFile?.takeIf { it.isBuildFile() }?.parent

  override fun getVirtualFile(): VirtualFile? = null

  override fun isValid(): Boolean = true

  override fun isPhysical(): Boolean = false

  override fun isDirectory(): Boolean = false

  override fun processChildren(psiElementProcessor: PsiElementProcessor<in PsiFileSystemItem>): Boolean = false
}

internal fun PsiFile.isBuildFile(): Boolean = name in Constants.BUILD_FILE_NAMES
