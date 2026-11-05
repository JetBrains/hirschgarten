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

import com.goide.psi.impl.GoPackage
import com.goide.psi.impl.imports.GoImportReference
import com.goide.psi.impl.imports.GoImportResolver
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import com.intellij.util.ThreeState
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity
import org.jetbrains.bazel.workspacemodel.entities.ImportPathId

internal class BazelGoImportResolver : GoImportResolver {
  override fun resolve(
    importPath: String,
    project: Project,
    module: Module?,
    resolveState: ResolveState?,
  ): Collection<GoPackage>? {
    if (!supportsGo(project)) return null
    val goPackage = resolveGoPackage(project, importPath)
    return if (goPackage != null) listOf(goPackage) else null
  }

  override fun supportsRelativeImportPaths(project: Project, module: Module?): ThreeState {
    if (!supportsGo(project)) return ThreeState.UNSURE
    return ThreeState.NO
  }

  override fun supportsInternalPackages(project: Project, module: Module?): ThreeState {
    if (!supportsGo(project)) return ThreeState.UNSURE
    return ThreeState.NO
  }

  override fun supportsSdkInternalPackages(project: Project, module: Module?): ThreeState {
    if (!supportsGo(project)) return ThreeState.UNSURE
    return ThreeState.NO
  }

  override fun resolve(reference: GoImportReference): Array<ResolveResult>? {
    val project = reference.element.project
    if (!supportsGo(project)) return null
    val importPath = reference.fileReferenceSet.pathString
    val goPackage = resolveGoPackage(project, importPath) ?: return null
    return doResolve(goPackage, reference.index)
  }

  private fun supportsGo(project: Project): Boolean =
    project.isBazelProject
}

private fun resolveGoPackage(project: Project, importPath: String): BazelGoPackage? {
  val entity: BazelGoPackageEntity = project.workspaceModel.currentSnapshot.resolve(ImportPathId(importPath)) ?: return null
  return BazelGoPackage(project, entity)
}

private fun doResolve(goPackage: BazelGoPackage, index: Int): Array<ResolveResult> =
  listOf(goPackage)
    .asSequence()
    .mapNotNull { it.getImportReferences() }
    .filter { index < it.size }
    .map { it[index] }
    .mapNotNull { GoPackageFileSystemItem.getInstance(it) }
    .map { PsiElementResolveResult(it) }
    .toList()
    .toTypedArray()
