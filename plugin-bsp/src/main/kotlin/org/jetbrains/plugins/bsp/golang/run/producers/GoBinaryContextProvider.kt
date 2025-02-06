/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.plugins.bsp.golang.run.producers

import com.google.idea.blaze.base.dependencies.TargetInfo

/** Go-specific handler for [BlazeCommandRunConfiguration]s.  */
internal class GoBinaryContextProvider : BinaryContextProvider {
  public override fun getRunContext(context: com.intellij.execution.actions.ConfigurationContext): BinaryRunContext? {
    val file: com.intellij.psi.PsiFile? =
      com.google.idea.blaze.golang.run.producers.GoBinaryContextProvider.Companion.getMainFile(context)
    if (file == null) {
      return null
    }
    val binaryTarget: TargetInfo? =
      com.google.idea.blaze.golang.run.producers.GoBinaryContextProvider.Companion.getTargetLabel(file)
    if (binaryTarget == null) {
      return null
    }
    return BinaryRunContext.create(file, binaryTarget)
  }

  companion object {
    private fun getMainFile(context: com.intellij.execution.actions.ConfigurationContext): com.intellij.psi.PsiFile? {
      val element: com.intellij.psi.PsiElement? = context.getPsiLocation()
      if (element == null) {
        return null
      }
      val file: com.intellij.psi.PsiFile? = element.getContainingFile()
      if (file is GoFile && GoRunUtil.isMainGoFile(file)) {
        return file
      }
      return null
    }

    private fun getTargetLabel(psiFile: com.intellij.psi.PsiFile): TargetInfo? {
      val project: com.intellij.openapi.project.Project = psiFile.getProject()
      val projectData: BlazeProjectData? =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
      if (projectData == null) {
        return null
      }
      val virtualFile: com.intellij.openapi.vfs.VirtualFile? = psiFile.getVirtualFile()
      if (virtualFile == null) {
        return null
      }
      val file: java.io.File = com.intellij.openapi.vfs.VfsUtil.virtualToIoFile(virtualFile)
      val rulesForFile: MutableCollection<TargetKey?> =
        SourceToTargetMap.getInstance(project).getRulesForSourceFile(file)

      val targetMap: TargetMap = projectData.getTargetMap()
      val libraryKeys: MutableList<TargetKey?> = java.util.ArrayList<TargetKey?>()
      for (key in rulesForFile) {
        val target: TargetIdeInfo? = targetMap.get(key)
        if (target == null || !target.getKind().hasLanguage(LanguageClass.GO)) {
          continue
        }
        when (target.getKind().getRuleType()) {
          BINARY -> return target.toTargetInfo()
          LIBRARY -> libraryKeys.add(target.getKey())
          TEST, UNKNOWN -> {}
        }
      }
      val rdeps: com.google.common.collect.ImmutableMultimap<TargetKey?, TargetKey?> =
        ReverseDependencyMap.get(project)
      return libraryKeys.stream()
        .map<com.google.common.collect.ImmutableCollection<TargetKey?>?> { key: TargetKey? -> rdeps.get(key) }
        .flatMap<TargetKey?> { obj: com.google.common.collect.ImmutableCollection<TargetKey?>? -> obj.stream() }
        .map<Any?>(targetMap::get)
        .filter { obj: Any? -> java.util.Objects.nonNull(obj) }
        .filter { t: Any? -> t.getKind().hasLanguage(LanguageClass.GO) }
        .filter { t: Any? -> t.getKind().getRuleType() === RuleType.BINARY }
        .map<Any?>(TargetIdeInfo::toTargetInfo)
        .findFirst()
        .orElse(null)
    }
  }
}
