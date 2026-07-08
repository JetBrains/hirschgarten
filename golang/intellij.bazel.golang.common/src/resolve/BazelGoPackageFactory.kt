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

import com.goide.project.GoPackageFactory
import com.goide.psi.GoFile
import com.goide.psi.impl.GoPackage
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.psi.PsiDirectory
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspacemodel.entities.BazelGoTargetEntityId
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabel

@ApiStatus.Internal
class BazelGoPackageFactory : GoPackageFactory {
  override fun createPackage(goFile: GoFile): GoPackage? {
    if (!BazelFeatureFlags.isGoSupportEnabled) return null
    val project = goFile.project
    if (!project.isBazelProject) return null
    val virtualFile = goFile.virtualFile ?: return null
    val targetUtils = project.targetUtils

    val containingTargets = targetUtils.getTargetsForFile(virtualFile)
    val snapshot = project.workspaceModel.currentSnapshot
    val goPackageEntity = containingTargets
                            .mapNotNull { label -> snapshot.resolve(BazelGoTargetEntityId(WorkspaceModelTargetLabel(label))) }
                            .firstNotNullOfOrNull { snapshot.resolve(it.importPath) } ?: return null
    return BazelGoPackage(project, goPackageEntity)
  }

  override fun createPackage(packageName: String, vararg directories: PsiDirectory): GoPackage? = null
}
