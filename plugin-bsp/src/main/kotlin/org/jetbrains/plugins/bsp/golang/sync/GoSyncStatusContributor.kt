/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
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
package org.jetbrains.plugins.bsp.golang.sync

import com.google.idea.blaze.base.model.BlazeProjectData

internal class GoSyncStatusContributor : SyncStatusContributor {
  public override fun toPsiFileAndName(
    projectData: BlazeProjectData?,
    node: com.intellij.ide.projectView.ProjectViewNode<*>?
  ): PsiFileAndName? {
    if (node !is com.intellij.ide.projectView.impl.nodes.PsiFileNode) {
      return null
    }
    val psiFile: com.intellij.psi.PsiFile? =
      (node as com.intellij.ide.projectView.impl.nodes.PsiFileNode).getValue()
    if (psiFile !is GoFile) {
      return null
    }
    return PsiFileAndName(psiFile, psiFile.getName())
  }

  public override fun handlesFile(
    projectData: BlazeProjectData,
    file: com.intellij.openapi.vfs.VirtualFile
  ): Boolean {
    return projectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.GO)
      && file.getName().endsWith(".go")
  }
}
